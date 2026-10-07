#!/usr/bin/env python3
"""
Multi-Batch Parallel Crawler Dashboard for KordApp (15 Batches)
FastAPI backend with embedded single-page reactive UI.
Supports running multiple batches concurrently (parallel threads).
"""
from __future__ import annotations

import csv
import glob
import json
import lzma
import os
import re
import sqlite3
import sys
import threading
import time
from collections import deque
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

# Setup paths
TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.fetcher import fetch_html
from crawler.parsers.guitartuna import parse_gt_html
from crawler.parsers.ultimate_guitar import parse_ug_html
from crawler.batch_crawler import (
    SCHEMA_SQL, load_gt_artist_index, discover_gt_songs, discover_ug_songs
)

app = FastAPI(title="KordApp Parallel Crawler Dashboard")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

BATCH_CSV_DIR = TOOLS_DIR / "catalog" / "batches"
BATCH_DB_DIR = TOOLS_DIR / "catalog" / "db_batches"
BATCH_DB_DIR.mkdir(parents=True, exist_ok=True)
MERGED_DB = TOOLS_DIR / "catalog" / "full_merged_catalog.db"
ASSET_XZ = TOOLS_DIR.parent / "app" / "src" / "main" / "assets" / "catalog.db.xz"

# Global state supporting multi-batch concurrency
class CrawlerState:
    def __init__(self):
        # active_batches: dict[batch_id -> {artist, index, total, stopping}]
        self.active_batches: dict[int, dict] = {}
        self.threads: dict[int, threading.Thread] = {}
        self.logs: deque[str] = deque(maxlen=400)
        self.lock = threading.Lock()
        self.auto_chain: bool = True

    def add_log(self, text: str):
        with self.lock:
            timestamp = time.strftime("%H:%M:%S")
            self.logs.append(f"[{timestamp}] {text}")

state = CrawlerState()


def get_batch_artist_stats(batch_num: int) -> dict:
    csv_file = BATCH_CSV_DIR / f"batch_{batch_num:02d}.csv"
    db_file = BATCH_DB_DIR / f"batch_{batch_num:02d}.db"
    
    targets = []
    if csv_file.exists():
        try:
            with open(csv_file, "r", encoding="utf-8") as f:
                targets = [r["artist"].strip() for r in csv.DictReader(f) if r.get("artist")]
        except Exception:
            pass
    total_targets = len(targets)
    
    if not db_file.exists():
        return {
            "total_targets": total_targets,
            "completed_targets": 0,
            "song_count": 0,
            "status": "idle",
            "percent": 0
        }
        
    try:
        con = sqlite3.connect(str(db_file))
        cur = con.cursor()
        song_count = cur.execute("SELECT count(*) FROM songs").fetchone()[0]
        db_artists_lower = {row[0].strip().lower() for row in cur.execute("SELECT DISTINCT artist FROM songs").fetchall() if row[0]}
        
        has_bp = cur.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='batch_progress'").fetchone()[0]
        bp_artists = set()
        if has_bp:
            bp_artists = {row[0].strip().lower() for row in cur.execute("SELECT artist FROM batch_progress").fetchall() if row[0]}
            
        con.close()
        
        completed_count = 0
        for t in targets:
            tl = t.lower()
            if tl in bp_artists or any(tl in da or da in tl for da in db_artists_lower):
                completed_count += 1
                
        pct = round((completed_count / total_targets * 100)) if total_targets > 0 else 0
        
        if total_targets > 0 and completed_count >= total_targets:
            status = "completed"
        elif song_count > 0:
            status = "partial"
        else:
            status = "idle"
            
        return {
            "total_targets": total_targets,
            "completed_targets": completed_count,
            "song_count": song_count,
            "status": status,
            "percent": pct
        }
    except Exception:
        return {
            "total_targets": total_targets,
            "completed_targets": 0,
            "song_count": 0,
            "status": "idle",
            "percent": 0
        }


def get_batch_song_count(batch_num: int) -> int:
    return get_batch_artist_stats(batch_num)["song_count"]


def get_batch_artist_count(batch_num: int) -> int:
    return get_batch_artist_stats(batch_num)["total_targets"]


def worker_crawl_batch(batch_num: int):
    tag = f"B{batch_num:02d}"
    with state.lock:
        state.active_batches[batch_num] = {
            "artist": "Inisialisasi...",
            "index": 0,
            "total": 0,
            "stopping": False
        }
    state.add_log(f"🚀 [{tag}] Memulai crawling Batch {batch_num:02d}...")

    csv_file = BATCH_CSV_DIR / f"batch_{batch_num:02d}.csv"
    if not csv_file.exists():
        state.add_log(f"❌ [{tag}] File batch tidak ditemukan: {csv_file.name}")
        with state.lock:
            state.active_batches.pop(batch_num, None)
        return

    targets = []
    with open(csv_file, "r", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            art = (r.get("artist") or "").strip()
            reg = (r.get("region") or "BARAT").strip()
            if art:
                targets.append((art, reg))

    with state.lock:
        if batch_num in state.active_batches:
            state.active_batches[batch_num]["total"] = len(targets)

    db_path = str(BATCH_DB_DIR / f"batch_{batch_num:02d}.db")
    conn = sqlite3.connect(db_path)
    conn.executescript(SCHEMA_SQL)
    conn.commit()

    # Pre-fetch existing artists in DB for instant skip / resume
    db_artists_lower = {row[0].strip().lower() for row in conn.execute("SELECT DISTINCT artist FROM songs").fetchall() if row[0]}

    gt_index = load_gt_artist_index()

    try:
        for idx, (artist_name, region) in enumerate(targets, 1):
            with state.lock:
                batch_info = state.active_batches.get(batch_num, {})
                if batch_info.get("stopping", False):
                    state.add_log(f"⏹ [{tag}] Dihentikan oleh pengguna.")
                    break
                batch_info["artist"] = artist_name
                batch_info["index"] = idx

            # Check if artist already scraped to enable instant resume
            art_lower = artist_name.lower().strip()
            if any(art_lower in da or da in art_lower for da in db_artists_lower):
                state.add_log(f"⏩ [{tag} {idx}/{len(targets)}] Lewati: {artist_name} (sudah ada di DB)")
                continue

            state.add_log(f"👉 [{tag} {idx}/{len(targets)}] Mulai: {artist_name} ({region})")

            # 1. Discover GT
            gt_url = gt_index.get(artist_name.lower().strip())
            gt_targets = []
            if gt_url:
                try:
                    gt_targets = discover_gt_songs(artist_name, gt_url)
                except Exception as e:
                    state.add_log(f"   ! [{tag} GT DISC ERR] {e}")

            # 2. Discover UG
            ug_targets = []
            try:
                ug_targets = discover_ug_songs(artist_name, max_pages=10)
            except Exception as e:
                state.add_log(f"   ! [{tag} UG DISC ERR] {e}")

            state.add_log(f"   🔍 [{tag}] Ditemukan: {len(gt_targets)} GT | {len(ug_targets)} UG")

            # 3. Scrape GT songs
            for item in gt_targets:
                if state.active_batches.get(batch_num, {}).get("stopping", False):
                    break
                url = item["url"]
                try:
                    c = conn.cursor()
                    c.execute("SELECT id FROM songs WHERE source_url = ?", (url,))
                    if c.fetchone():
                        continue

                    html_content = fetch_html(url)
                    if not html_content:
                        continue
                    parsed = parse_gt_html(html_content, url)
                    if parsed and parsed.content and parsed.chords:
                        c.execute("""
                            INSERT OR IGNORE INTO songs (
                                source, source_url, title, artist, title_slug, artist_slug,
                                version_name, rating, votes, key_base, difficulty, category,
                                region, chords, chord_count, line_count, content, content_hash, scraped_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, (
                            parsed.source, parsed.source_url, parsed.title, parsed.artist,
                            parsed.title_slug, parsed.artist_slug, item["version_type"],
                            parsed.rating, parsed.votes, parsed.key_base, parsed.difficulty,
                            parsed.category, region, ",".join(parsed.chords), parsed.chord_count,
                            parsed.line_count, parsed.content, parsed.content_hash, parsed.scraped_at
                        ))
                        conn.commit()
                        state.add_log(f"   + [{tag} GT] {parsed.artist} - {parsed.title} ({item['version_type']})")
                    time.sleep(0.2)
                except Exception as e:
                    state.add_log(f"   ! [{tag} GT ERR] {url}: {e}")

            # 4. Scrape UG songs
            for item in ug_targets:
                if state.active_batches.get(batch_num, {}).get("stopping", False):
                    break
                url = item["url"]
                try:
                    c = conn.cursor()
                    c.execute("SELECT id FROM songs WHERE source_url = ?", (url,))
                    if c.fetchone():
                        continue

                    html_content = fetch_html(url)
                    if not html_content:
                        continue
                    parsed = parse_ug_html(html_content, url, artist_hint=artist_name, title_hint=item["title"])
                    if parsed and parsed.content and parsed.chords:
                        c.execute("""
                            INSERT OR IGNORE INTO songs (
                                source, source_url, title, artist, title_slug, artist_slug,
                                version_name, rating, votes, key_base, difficulty, category,
                                region, chords, chord_count, line_count, content, content_hash, scraped_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, (
                            parsed.source, parsed.source_url, parsed.title, parsed.artist,
                            parsed.title_slug, parsed.artist_slug, item["version_type"],
                            parsed.rating or item.get("rating"), parsed.votes or item.get("votes"),
                            parsed.key_base, parsed.difficulty, parsed.category, region,
                            ",".join(parsed.chords), parsed.chord_count, parsed.line_count,
                            parsed.content, parsed.content_hash, parsed.scraped_at
                        ))
                        conn.commit()
                        state.add_log(f"   + [{tag} UG] {parsed.artist} - {parsed.title} ({item['version_type']})")
                    time.sleep(0.25)
                except Exception as e:
                    state.add_log(f"   ! [{tag} UG ERR] {url}: {e}")

        conn.close()
        is_stopped = state.active_batches.get(batch_num, {}).get("stopping", False)
        if not is_stopped:
            state.add_log(f"🎉 [{tag}] Selesai! Seluruh artis di Batch {batch_num:02d} selesai diproses.")
    except Exception as e:
        state.add_log(f"❌ [{tag} EXCEPTION] {e}")
    finally:
        with state.lock:
            state.active_batches.pop(batch_num, None)
            state.threads.pop(batch_num, None)


def normalize_artist_and_title(artist: str, title: str) -> tuple[str, str]:
    artist = (artist or "").strip()
    title = (title or "").strip()
    
    # 1. Clean double duplicate name separated by comma (GT artifact: "BILLIE EILISH, BILLIE EILISH")
    if "," in artist:
        parts = [p.strip() for p in artist.split(",") if p.strip()]
        if len(parts) >= 2 and parts[0].lower() == parts[1].lower():
            artist = parts[0]
            
    # 2. Check feat / featuring in artist name
    has_feat_in_title = bool(re.search(r"\b(feat\.?|ft\.?|featuring)\b", title, re.I))
    m_feat = re.search(r"^(.*?)\s+(?:feat\.?|ft\.?|featuring)\s+(.*)$", artist, re.I)
    if m_feat:
        main_art = m_feat.group(1).strip()
        feat_art = m_feat.group(2).strip()
        if not has_feat_in_title and feat_art:
            title = f"{title} (feat. {feat_art})"
        artist = main_art

    return artist, title


@app.get("/api/status")
def get_status():
    batches = []
    total_songs_all = 0
    total_completed_batches = 0
    total_completed_artists = 0
    total_all_artists = 0
    with state.lock:
        active_copy = dict(state.active_batches)

    for i in range(1, 16):
        stats = get_batch_artist_stats(i)
        is_running = (i in active_copy)
        info = active_copy.get(i, {})
        
        status = "running" if is_running else stats["status"]
        if stats["status"] == "completed":
            total_completed_batches += 1
            
        total_songs_all += stats["song_count"]
        total_completed_artists += stats["completed_targets"]
        total_all_artists += stats["total_targets"]
        
        batches.append({
            "id": i,
            "name": f"Batch {i:02d}",
            "artist_count": stats["total_targets"],
            "completed_artists": stats["completed_targets"],
            "song_count": stats["song_count"],
            "status": status,
            "percent": stats["percent"],
            "is_running": is_running,
            "current_artist": info.get("artist", ""),
            "current_index": info.get("index", stats["completed_targets"]),
            "total_artists": stats["total_targets"]
        })

    return {
        "active_batches_count": len(active_copy),
        "active_batch_ids": sorted(list(active_copy.keys())),
        "total_songs_all": total_songs_all,
        "completed_batches_count": total_completed_batches,
        "completed_artists_count": total_completed_artists,
        "total_artists_count": total_all_artists,
        "batches": batches
    }


@app.get("/api/logs")
def get_logs():
    with state.lock:
        return {"logs": list(state.logs)}


@app.post("/api/batch/{batch_id}/start")
def start_batch(batch_id: int):
    with state.lock:
        if batch_id in state.active_batches:
            raise HTTPException(status_code=400, detail=f"Batch {batch_id} sudah sedang berjalan.")
        t = threading.Thread(target=worker_crawl_batch, args=(batch_id,), daemon=True)
        state.threads[batch_id] = t
        t.start()
    return {"status": "ok", "message": f"Batch {batch_id} dimulai"}


@app.post("/api/batch/{batch_id}/stop")
def stop_batch(batch_id: int):
    with state.lock:
        if batch_id in state.active_batches:
            state.active_batches[batch_id]["stopping"] = True
            state.add_log(f"⏳ Mengirim sinyal berhenti ke Batch {batch_id:02d}...")
            return {"status": "stopping", "message": f"Batch {batch_id} sedang dihentikan"}
    return {"status": "idle", "message": f"Batch {batch_id} tidak sedang berjalan"}


@app.post("/api/batch/stop")
def stop_all_batches():
    with state.lock:
        if not state.active_batches:
            return {"status": "idle", "message": "Tidak ada batch yang sedang berjalan"}
        for b_id, info in state.active_batches.items():
            info["stopping"] = True
        state.add_log("⏳ Mengirim sinyal berhenti ke SEMUA batch aktif...")
    return {"status": "stopping", "message": "Semua batch aktif sedang dihentikan"}


@app.post("/api/turbo")
def start_turbo(count: int = 3):
    """Starts the next unstarted batches in parallel (up to count batches)."""
    started = []
    with state.lock:
        running = set(state.active_batches.keys())
        for b_id in range(1, 16):
            if len(started) >= count:
                break
            if b_id not in running:
                t = threading.Thread(target=worker_crawl_batch, args=(b_id,), daemon=True)
                state.threads[b_id] = t
                t.start()
                started.append(b_id)
    
    state.add_log(f"⚡ [TURBO MODE] Menjalankan {len(started)} Batch sekaligus: {started}!")
    return {"status": "ok", "started_batches": started}


@app.post("/api/merge")
def merge_catalog():
    if state.active_batches:
        raise HTTPException(status_code=400, detail="Masih ada batch yang sedang berjalan. Tunggu selesai atau hentikan dulu.")
    
    state.add_log("📦 Memulai penggabungan seluruh batch database...")
    batch_dbs = sorted(glob.glob(str(BATCH_DB_DIR / "batch_*.db")))
    if not batch_dbs:
        state.add_log("⚠️ Belum ada file database batch untuk digabung.")
        return {"status": "error", "message": "Belum ada batch yang selesai"}

    if MERGED_DB.exists():
        MERGED_DB.unlink()

    conn_out = sqlite3.connect(MERGED_DB)
    conn_out.executescript(SCHEMA_SQL)
    conn_out.commit()

    total_merged = 0
    for b_path in batch_dbs:
        b_name = Path(b_path).name
        conn_in = sqlite3.connect(b_path)
        try:
            rows = conn_in.execute("""
                SELECT source, source_url, title, artist, title_slug, artist_slug,
                       version_name, rating, votes, key_base, difficulty, category,
                       region, chords, chord_count, line_count, content, content_hash, scraped_at
                FROM songs
            """).fetchall()
            c = conn_out.cursor()
            inserted = 0
            for r in rows:
                try:
                    source, source_url, title, artist, title_slug, artist_slug, version_name, rating, votes, key_base, difficulty, category, region, chords, chord_count, line_count, content, content_hash, scraped_at = r
                    
                    clean_art, clean_title = normalize_artist_and_title(artist, title)
                    if clean_art != artist or clean_title != title:
                        artist = clean_art
                        title = clean_title
                        artist_slug = re.sub(r"[^a-zA-Z0-9]+", "-", artist.lower()).strip("-")
                        title_slug = re.sub(r"[^a-zA-Z0-9]+", "-", title.lower()).strip("-")

                    c.execute("""
                        INSERT OR IGNORE INTO songs (
                            source, source_url, title, artist, title_slug, artist_slug,
                            version_name, rating, votes, key_base, difficulty, category,
                            region, chords, chord_count, line_count, content, content_hash, scraped_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        source, source_url, title, artist, title_slug, artist_slug,
                        version_name, rating, votes, key_base, difficulty, category,
                        region, chords, chord_count, line_count, content, content_hash, scraped_at
                    ))
                    if c.rowcount > 0:
                        inserted += 1
                except Exception:
                    pass
            conn_out.commit()
            total_merged += inserted
            state.add_log(f"   + Berhasil menggabung {b_name}: {inserted} lagu.")
        finally:
            conn_in.close()

    conn_out.close()

    # Package into catalog.db.xz
    state.add_log(f"🗜️ Mengompres {total_merged} lagu ke assets: {ASSET_XZ.name}...")
    with open(MERGED_DB, "rb") as f_in:
        raw_bytes = f_in.read()
    with lzma.open(ASSET_XZ, "wb") as f_out:
        f_out.write(raw_bytes)

    state.add_log(f"✅ Selesai! Katalog aplikasi berhasil diperbarui ({os.path.getsize(ASSET_XZ) // 1024} KB).")
    return {"status": "ok", "total_merged": total_merged}


# Embedded Clean & Modern Multi-Batch Parallel UI
HTML_PAGE = """<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>KordApp Parallel Crawler Monitor</title>
  <style>
    :root {
      --bg: #0b0f19;
      --card-bg: #151d2f;
      --card-border: #232f48;
      --card-hover: #1c273e;
      --primary: #3b82f6;
      --primary-hover: #2563eb;
      --purple: #8b5cf6;
      --purple-hover: #7c3aed;
      --green: #10b981;
      --green-bg: #064e3b;
      --yellow: #f59e0b;
      --red: #ef4444;
      --red-bg: #7f1d1d;
      --text: #f8fafc;
      --text-muted: #94a3b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: var(--bg);
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      padding: 20px;
      line-height: 1.5;
    }
    .container { max-width: 1240px; margin: 0 auto; }
    
    /* Header */
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 14px;
      padding: 16px 24px;
      margin-bottom: 20px;
      flex-wrap: wrap;
      gap: 14px;
    }
    .brand h1 { font-size: 1.35rem; font-weight: 700; color: #fff; display: flex; align-items: center; gap: 10px; }
    .brand p { font-size: 0.82rem; color: var(--text-muted); margin-top: 2px; }
    
    .header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .stat-badge {
      background: #1e293b;
      border: 1px solid #334155;
      padding: 6px 14px;
      border-radius: 10px;
      text-align: right;
    }
    .stat-badge span { font-size: 0.7rem; color: var(--text-muted); display: block; text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-badge strong { font-size: 1.15rem; color: var(--green); }
    
    .btn {
      padding: 9px 16px;
      font-size: 0.88rem;
      font-weight: 600;
      border-radius: 10px;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    .btn-turbo { background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%); color: #fff; box-shadow: 0 0 15px rgba(236,72,153,0.3); }
    .btn-turbo:hover { transform: translateY(-1px); box-shadow: 0 0 20px rgba(236,72,153,0.5); }
    .btn-primary { background: var(--primary); color: #fff; }
    .btn-primary:hover { background: var(--primary-hover); transform: translateY(-1px); }
    .btn-danger { background: var(--red); color: #fff; }
    .btn-danger:hover { background: #dc2626; }
    .btn-success { background: var(--green); color: #fff; }
    .btn-success:hover { background: #059669; }
    .btn-completed { background: #064e3b; color: #34d399; border: 1px solid #059669; }
    .btn-completed:hover { background: #047857; color: #fff; }
    .btn-warning { background: #d97706; color: #fff; }
    .btn-warning:hover { background: #b45309; }
    .btn-disabled { opacity: 0.5; cursor: not-allowed; }

    /* Live Status Banner */
    .active-banner {
      background: linear-gradient(135deg, #1e3a8a 0%, #1e1b4b 100%);
      border: 1px solid #3b82f6;
      border-radius: 12px;
      padding: 14px 20px;
      margin-bottom: 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }
    .active-banner.idle {
      background: var(--card-bg);
      border-color: var(--card-border);
    }
    .banner-left { display: flex; align-items: center; gap: 12px; }
    .pulse-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: var(--green);
      box-shadow: 0 0 10px var(--green);
      animation: pulse 1.5s infinite;
    }
    .pulse-dot.idle { background: #64748b; box-shadow: none; animation: none; }
    @keyframes pulse {
      0% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(1.2); }
      100% { opacity: 1; transform: scale(1); }
    }

    /* Grid Batches */
    .section-title { font-size: 1.05rem; font-weight: 600; margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
      gap: 12px;
      margin-bottom: 24px;
    }
    .card {
      background: var(--card-bg);
      border: 1px solid var(--card-border);
      border-radius: 12px;
      padding: 14px;
      transition: all 0.2s;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .card:hover { border-color: #3b82f6; background: var(--card-hover); }
    .card.running { border-color: var(--green); background: #0c271e; box-shadow: 0 0 14px rgba(16,185,129,0.25); }
    .card.completed { border-color: #05966966; background: #0b1f16; }
    .card.partial { border-color: #d9770644; background: #1a1711; }
    
    .card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
    .card-top h3 { font-size: 0.95rem; font-weight: 700; color: #fff; }
    .badge-status {
      font-size: 0.72rem;
      padding: 3px 8px;
      border-radius: 6px;
      font-weight: 700;
      background: #1e293b;
      color: var(--text-muted);
      letter-spacing: 0.3px;
    }
    .badge-status.running { background: #1e3a8a; color: #60a5fa; border: 1px solid #3b82f6; }
    .badge-status.success { background: #064e3b; color: #34d399; border: 1px solid #059669; }
    .badge-status.partial { background: #451a03; color: #fbbf24; border: 1px solid #d97706; }
    .badge-status.idle { background: #1e293b; color: #94a3b8; }

    /* Progress bar */
    .progress-track {
      height: 6px;
      width: 100%;
      background: #1e293b;
      border-radius: 4px;
      overflow: hidden;
      margin: 6px 0 10px 0;
    }
    .progress-fill {
      height: 100%;
      transition: width 0.3s ease;
    }
    .progress-fill.completed { background: #10b981; }
    .progress-fill.partial { background: #f59e0b; }
    .progress-fill.running { background: #3b82f6; }
    .progress-fill.idle { background: #334155; }

    .card-info { font-size: 0.78rem; color: var(--text-muted); margin-bottom: 12px; }
    .card-info div { margin-bottom: 2px; }
    .card-info strong { color: #fff; }
    .card-now { font-size: 0.75rem; color: #34d399; margin-top: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
    .card-actions { margin-top: auto; }
    .card-actions button { width: 100%; justify-content: center; font-size: 0.82rem; padding: 8px 10px; }

    /* Terminal Console */
    .terminal-container {
      background: #060911;
      border: 1px solid var(--card-border);
      border-radius: 12px;
      overflow: hidden;
    }
    .terminal-header {
      background: #0f172a;
      padding: 10px 16px;
      font-size: 0.85rem;
      font-weight: 600;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--card-border);
    }
    .terminal-body {
      padding: 14px;
      height: 300px;
      overflow-y: auto;
      font-family: Consolas, "Fira Code", monospace;
      font-size: 0.82rem;
      line-height: 1.6;
      color: #e2e8f0;
      display: flex;
      flex-direction: column;
    }
    .log-line { word-break: break-all; }
    .log-line.ok { color: #34d399; }
    .log-line.gt { color: #38bdf8; }
    .log-line.err { color: #f87171; }
    .log-line.info { color: #fbbf24; }
  </style>
</head>
<body>
  <div class="container">
    <!-- Header -->
    <header>
      <div class="brand">
        <h1>⚡ KordApp Parallel Crawler</h1>
        <p>Bisa menjalankan beberapa batch sekaligus secara paralel (Multi-Thread)</p>
      </div>
      <div class="header-actions">
        <div class="stat-badge">
          <span>Batch Selesai</span>
          <strong id="stat-batches-done" style="color:#10b981;">0 / 15</strong>
        </div>
        <div class="stat-badge">
          <span>Artis Selesai</span>
          <strong id="stat-artists-done" style="color:#38bdf8;">0 / 813</strong>
        </div>
        <div class="stat-badge">
          <span>Total Lagu Terkumpul</span>
          <strong id="stat-total-songs" style="color:#fbbf24;">0 Lagu</strong>
        </div>
        <button class="btn btn-turbo" onclick="startTurbo(2)">⚡ Turbo (2 Batch Sekaligus)</button>
        <button class="btn btn-primary" onclick="mergeCatalog()">📦 Gabungkan ke APK Assets</button>
      </div>
    </header>

    <!-- Active Banner -->
    <div id="active-banner" class="active-banner idle">
      <div class="banner-left">
        <div id="banner-dot" class="pulse-dot idle"></div>
        <div>
          <strong id="banner-title" style="font-size: 1.05rem;">Status: Menganggur</strong>
          <p id="banner-desc" style="font-size: 0.85rem; color: var(--text-muted);">Klik tombol batch manapun untuk melanjutkan sisa artis atau memulai batch baru.</p>
        </div>
      </div>
      <div id="banner-action">
        <!-- Injected dynamically -->
      </div>
    </div>

    <!-- 15 Batches Grid -->
    <div class="section-title">
      <span>Daftar 15 Batch Katalog Artis</span>
      <span style="font-size: 0.8rem; color: var(--text-muted);">Batch yang sudah 100% selesai ditandai hijau dengan label <strong>✅ SUKSES SELESAI</strong></span>
    </div>
    <div class="grid" id="batches-grid">
      <!-- Cards rendered via JS -->
    </div>

    <!-- Realtime Logs Terminal -->
    <div class="terminal-container">
      <div class="terminal-header">
        <span>📟 Live Activity Monitor (Menampilkan Aktivitas Seluruh Batch)</span>
        <button style="background:transparent; border:none; color:var(--text-muted); cursor:pointer; font-size:0.75rem;" onclick="clearLogs()">Clear Screen</button>
      </div>
      <div class="terminal-body" id="terminal-body">
        <div class="log-line info">[SISTEM] Monitor siap. Setiap batch berjalan di worker mandiri.</div>
      </div>
    </div>
  </div>

  <script>
    async function fetchStatus() {
      try {
        const res = await fetch('/api/status');
        const data = await res.json();
        renderStatus(data);
      } catch (err) {
        console.error("Gagal mengambil status:", err);
      }
    }

    async function fetchLogs() {
      try {
        const res = await fetch('/api/logs');
        const data = await res.json();
        renderLogs(data.logs);
      } catch (err) {
        console.error("Gagal mengambil log:", err);
      }
    }

    function renderStatus(data) {
      document.getElementById('stat-total-songs').textContent = Number(data.total_songs_all).toLocaleString() + ' Lagu';
      document.getElementById('stat-batches-done').textContent = `${data.completed_batches_count} / 15 Batch`;
      document.getElementById('stat-artists-done').textContent = `${data.completed_artists_count} / ${data.total_artists_count} Artis`;

      const banner = document.getElementById('active-banner');
      const dot = document.getElementById('banner-dot');
      const title = document.getElementById('banner-title');
      const desc = document.getElementById('banner-desc');
      const action = document.getElementById('banner-action');

      const count = data.active_batches_count;
      if (count > 0) {
        banner.className = 'active-banner';
        dot.className = 'pulse-dot';
        const batchListStr = data.active_batch_ids.map(id => `Batch ${String(id).padStart(2, '0')}`).join(', ');
        title.textContent = `⚡ Sedang Berjalan: ${count} Batch Paralel (${batchListStr})`;
        desc.textContent = 'Masing-masing batch bekerja secara independen di thread terpisah.';
        action.innerHTML = `<button class="btn btn-danger" onclick="stopAll()">⏹ Hentikan Semua</button>`;
      } else {
        banner.className = 'active-banner idle';
        dot.className = 'pulse-dot idle';
        title.textContent = 'Status: Menganggur (Siap Dijalankan)';
        desc.textContent = 'Klik tombol batch manapun atau gunakan Turbo Mode untuk menjalankan beberapa batch sekaligus.';
        action.innerHTML = '';
      }

      const grid = document.getElementById('batches-grid');
      grid.innerHTML = '';

      data.batches.forEach(b => {
        const card = document.createElement('div');
        const isRunning = b.is_running;
        
        let cardClass = 'card';
        let badgeHtml = '';
        let buttonHtml = '';
        let fillClass = 'idle';

        if (isRunning) {
          cardClass += ' running';
          badgeHtml = `<span class="badge-status running">⚡ RUNNING [${b.current_index}/${b.total_artists}]</span>`;
          buttonHtml = `<button class="btn btn-danger" onclick="stopSingle(${b.id})">⏹ Stop Batch ${b.id}</button>`;
          fillClass = 'running';
        } else if (b.status === 'completed') {
          cardClass += ' completed';
          badgeHtml = `<span class="badge-status success">✅ SUKSES SELESAI</span>`;
          buttonHtml = `<button class="btn btn-completed" onclick="startSingle(${b.id})" title="Batch ini sudah 100% selesai. Klik jika ingin scan ulang.">✓ Selesai (${b.completed_artists}/${b.artist_count} Artis)</button>`;
          fillClass = 'completed';
        } else if (b.status === 'partial') {
          cardClass += ' partial';
          badgeHtml = `<span class="badge-status partial">⏸ SEBAGIAN (${b.percent}%)</span>`;
          const remaining = b.artist_count - b.completed_artists;
          buttonHtml = `<button class="btn btn-warning" onclick="startSingle(${b.id})">▶ Lanjutkan (${remaining} Sisa Artis)</button>`;
          fillClass = 'partial';
        } else {
          badgeHtml = `<span class="badge-status idle">⚪ BELUM DIMULAI</span>`;
          buttonHtml = `<button class="btn btn-primary" onclick="startSingle(${b.id})">▶ Mulai Batch ${b.id}</button>`;
          fillClass = 'idle';
        }

        let nowHtml = '';
        if (isRunning && b.current_artist) {
          nowHtml = `<div class="card-now" title="${b.current_artist}">👉 ${b.current_artist}</div>`;
        }

        card.className = cardClass;
        card.innerHTML = `
          <div>
            <div class="card-top">
              <h3>${b.name}</h3>
              ${badgeHtml}
            </div>
            <div class="progress-track">
              <div class="progress-fill ${fillClass}" style="width: ${b.percent}%"></div>
            </div>
            <div class="card-info">
              <div>Artis Selesai: <strong>${b.completed_artists} / ${b.artist_count} Artis (${b.percent}%)</strong></div>
              <div>Lagu Tersimpan: <strong>${Number(b.song_count).toLocaleString()} Lagu</strong></div>
              ${nowHtml}
            </div>
          </div>
          <div class="card-actions">
            ${buttonHtml}
          </div>
        `;
        grid.appendChild(card);
      });
    }

    function renderLogs(logs) {
      const container = document.getElementById('terminal-body');
      container.innerHTML = '';
      logs.forEach(line => {
        const div = document.createElement('div');
        div.className = 'log-line';
        if (line.includes('[UG]') || line.includes('[UG OK]')) div.className += ' ok';
        else if (line.includes('[GT]') || line.includes('[GT OK]')) div.className += ' gt';
        else if (line.includes('ERR') || line.includes('❌')) div.className += ' err';
        else if (line.includes('Mulai') || line.includes('Selesai') || line.includes('TURBO')) div.className += ' info';
        div.textContent = line;
        container.appendChild(div);
      });
      container.scrollTop = container.scrollHeight;
    }

    async function startSingle(id) {
      try {
        const res = await fetch(`/api/batch/${id}/start`, { method: 'POST' });
        if (!res.ok) {
          const err = await res.json();
          alert(err.detail || "Gagal memulai batch");
        }
        fetchStatus();
      } catch (err) {
        alert("Gagal start batch: " + err);
      }
    }

    async function stopSingle(id) {
      try {
        await fetch(`/api/batch/${id}/stop`, { method: 'POST' });
        fetchStatus();
      } catch (err) {
        alert("Gagal stop batch: " + err);
      }
    }

    async function stopAll() {
      try {
        await fetch('/api/batch/stop', { method: 'POST' });
        fetchStatus();
      } catch (err) {
        alert("Gagal stop semua batch: " + err);
      }
    }

    async function startTurbo(count) {
      try {
        const res = await fetch(`/api/turbo?count=${count}`, { method: 'POST' });
        fetchStatus();
      } catch (err) {
        alert("Gagal start turbo: " + err);
      }
    }

    async function mergeCatalog() {
      if (!confirm("Gabungkan seluruh database batch yang ada ke dalam catalog.db.xz aplikasi?")) return;
      try {
        const res = await fetch('/api/merge', { method: 'POST' });
        const data = await res.json();
        if (res.ok) {
          alert(`Sukses! ${data.total_merged} lagu berhasil digabungkan ke assets APK.`);
        } else {
          alert(data.detail || "Gagal merge catalog");
        }
      } catch (err) {
        alert("Error merge: " + err);
      }
    }

    function clearLogs() {
      document.getElementById('terminal-body').innerHTML = '<div class="log-line info">[SISTEM] Layar dibersihkan.</div>';
    }

    setInterval(fetchStatus, 1200);
    setInterval(fetchLogs, 1200);
    fetchStatus();
    fetchLogs();
  </script>
</body>
</html>
"""

@app.get("/", response_class=HTMLResponse)
def index_page():
    return HTMLResponse(content=HTML_PAGE)

if __name__ == "__main__":
    print("\n" + "="*60)
    print("  KordApp Parallel Crawler Monitor Dashboard")
    print("  Buka browser di: http://localhost:8000")
    print("="*60 + "\n")
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level="warning")
