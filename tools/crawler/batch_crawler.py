#!/usr/bin/env python3
"""
Batch Crawler & Scraper for GuitarTuna (GT) and Ultimate Guitar (UG).
Implements the 2-version rule:
- GT: Easy Chords & Original Version
- UG: Top Votes & Best Rating
"""
from __future__ import annotations

import argparse
import csv
import html
import json
import lzma
import os
import re
import sqlite3
import sys
import time
import unicodedata
from collections import defaultdict
from pathlib import Path
from urllib.parse import quote, urlparse
from bs4 import BeautifulSoup

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

# Add tools to sys.path
TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.fetcher import fetch_html
from crawler.parsers.guitartuna import parse_gt_html
from crawler.parsers.ultimate_guitar import parse_ug_html
from crawler.models import ParsedSong

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS songs (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    source        TEXT NOT NULL,
    source_url    TEXT NOT NULL UNIQUE,
    title         TEXT NOT NULL,
    artist        TEXT NOT NULL,
    title_slug    TEXT NOT NULL,
    artist_slug   TEXT NOT NULL,
    version_name  TEXT,
    rating        REAL,
    votes         INTEGER,
    key_base      TEXT,
    difficulty    TEXT,
    category      TEXT,
    region        TEXT NOT NULL,
    chords        TEXT NOT NULL,
    chord_count   INTEGER NOT NULL,
    line_count    INTEGER NOT NULL,
    content       TEXT NOT NULL,
    content_hash  TEXT NOT NULL,
    scraped_at    TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_source ON songs (source);
CREATE INDEX IF NOT EXISTS idx_title_slug ON songs (title_slug);
CREATE INDEX IF NOT EXISTS idx_artist_slug ON songs (artist_slug);
"""

GT_INDEX_TSV = Path("C:/ChordApp/guitartuna_crawler/data/gt/artist_index.tsv")


def load_gt_artist_index() -> dict[str, str]:
    mapping = {}
    if GT_INDEX_TSV.exists():
        with open(GT_INDEX_TSV, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.strip().split("\t")
                if len(parts) == 2:
                    name, url = parts
                    mapping[name.lower().strip()] = url.strip()
    return mapping


def discover_gt_songs(artist_name: str, artist_url: str) -> list[dict]:
    """
    Scrapes the GT artist page and returns songs matching the artist.
    Applies the rule: 1 Easy + 1 Original version per song.
    """
    html_text = fetch_html(artist_url)
    if not html_text:
        print(f"  [GT] Failed to fetch artist page: {artist_url}")
        return []

    soup = BeautifulSoup(html_text, "html.parser")

    # Extract slug directly from artist_url if available
    m_slug = re.search(r"/artists/([a-zA-Z0-9_-]+?)-easy-original-guitar-chords-tabs", artist_url)
    if m_slug:
        artist_slug = m_slug.group(1).lower().strip()
    else:
        artist_slug = re.sub(r"[^a-zA-Z0-9]+", "-", artist_name.lower()).strip("-")
    # Group by song_key and song_hex so versions sharing the exact same song ID are paired together
    hex_groups: dict[str, dict[str, dict[str, str]]] = defaultdict(lambda: defaultdict(dict))

    for a in soup.find_all("a", href=True):
        href = a["href"]
        if "/chords/" not in href:
            continue

        # Match song href format: /chords/<song>-<artist>-<type>-guitar-chords-<id>
        if f"-{artist_slug}-" in href:
            m = re.search(rf"/chords/(.*?)-{artist_slug}-(easy|original)-guitar-chords-([0-9a-fA-F]+)", href)
            if m:
                song_key = m.group(1).lower().strip()
                v_type = m.group(2) # easy or original
                song_hex = m.group(3).lower().strip()
                full_url = "https://guitartuna.com" + href if href.startswith("/") else href
                hex_groups[song_key][song_hex][v_type] = full_url

    results = []
    for song_key, hex_map in hex_groups.items():
        title_guess = song_key.replace("-", " ").title()
        # Prioritize the song_hex that has BOTH easy and original versions
        best_hex = None
        for s_hex, vmap in hex_map.items():
            if "easy" in vmap and "original" in vmap:
                best_hex = s_hex
                break
        if not best_hex:
            best_hex = next(iter(hex_map.keys()))

        vmap = hex_map[best_hex]
        if "easy" in vmap:
            results.append({
                "source": "GT",
                "title": title_guess,
                "url": vmap["easy"],
                "version_type": "Easy Chords"
            })
        if "original" in vmap:
            results.append({
                "source": "GT",
                "title": title_guess,
                "url": vmap["original"],
                "version_type": "Original Version"
            })

    return results


UG_ALIASES = {
    "michael buble": "Michael Bublé",
    "beyonce": "Beyoncé",
    "motley crue": "Mötley Crüe",
    "motorhead": "Motörhead",
    "bjork": "Björk",
    "sigur ros": "Sigur Rós",
    "jose gonzalez": "José González",
    "sinead o'connor": "Sinéad O'Connor",
}


def strip_accents(s: str) -> str:
    return unicodedata.normalize('NFKD', s).encode('ascii', 'ignore').decode('utf-8').lower()


def discover_ug_songs(artist_name: str, max_pages: int = 10) -> list[dict]:
    """
    Searches UG for the artist and returns songs matching the rule:
    1 Top Votes + 1 Best Rating per song.
    Paginates through search results up to max_pages.
    """
    search_artist = UG_ALIASES.get(artist_name.lower().strip(), artist_name)
    encoded_artist = quote(search_artist)
    norm_artist = re.sub(r"[^\w\s]", "", strip_accents(artist_name)).strip()
    tabs = []
    
    current_page = 1
    total_pages = 1
    
    while current_page <= total_pages and current_page <= max_pages:
        url = f"https://www.ultimate-guitar.com/search.php?title={encoded_artist}&type=300&page={current_page}"
        html_text = fetch_html(url)
        if not html_text:
            break

        m = re.search(r'class="js-store" data-content="([^"]+)"', html_text)
        if not m:
            break

        try:
            data = json.loads(html.unescape(m.group(1)))
        except Exception:
            break

        page_data = data.get("store", {}).get("page", {}).get("data", {})
        results = page_data.get("results", [])
        pagination = page_data.get("pagination", {})
        total_pages = int(pagination.get("total") or 1)

        for item in results:
            if item.get("type") != "Chords":
                continue
            item_artist = re.sub(r"[^\w\s]", "", strip_accents(item.get("artist_name") or "")).strip()
            if item_artist == norm_artist or norm_artist in item_artist:
                tabs.append(item)

        current_page += 1

    # Group tabs by song name
    by_song: dict[str, list[dict]] = defaultdict(list)
    for t in tabs:
        song_name = (t.get("song_name") or "").strip()
        if song_name:
            by_song[song_name.lower()].append(t)

    selected = []
    for s_name, tab_list in by_song.items():
        # Rank by votes descending
        sorted_by_votes = sorted(tab_list, key=lambda x: int(x.get("votes") or 0), reverse=True)
        top_votes = sorted_by_votes[0]
        selected.append({
            "source": "UG",
            "title": top_votes.get("song_name"),
            "url": top_votes.get("tab_url"),
            "version_type": f"Ver {top_votes.get('version', 1)} (Top Votes)",
            "votes": int(top_votes.get("votes") or 0),
            "rating": float(top_votes.get("rating") or 0.0)
        })

        # Rank remaining by rating descending (minimum 3 votes to avoid 1-vote 5.0 anomaly)
        remaining = [x for x in tab_list if x.get("tab_url") != top_votes.get("tab_url")]
        if remaining:
            # Filter for reasonable votes, or fallback to all remaining
            qualified = [x for x in remaining if int(x.get("votes") or 0) >= 3]
            candidates = qualified if qualified else remaining
            sorted_by_rating = sorted(candidates, key=lambda x: (float(x.get("rating") or 0.0), int(x.get("votes") or 0)), reverse=True)
            best_rating = sorted_by_rating[0]
            selected.append({
                "source": "UG",
                "title": best_rating.get("song_name"),
                "url": best_rating.get("tab_url"),
                "version_type": f"Ver {best_rating.get('version', 2)} (Best Rating)",
                "votes": int(best_rating.get("votes") or 0),
                "rating": float(best_rating.get("rating") or 0.0)
            })

    return selected


def to_slug(text: str) -> str:
    s = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode('utf-8')
    return re.sub(r'[^a-zA-Z0-9]+', '-', s).strip('-').lower()


def crawl_and_build(target_artists: list[tuple[str, str]], output_db: str = "fresh_catalog.db"):
    """
    Crawls songs for target_artists and saves into output_db.
    """
    gt_index = load_gt_artist_index()

    conn = sqlite3.connect(output_db)
    conn.executescript(SCHEMA_SQL)
    conn.commit()

    total_scraped = 0

    for artist_name, region in target_artists:
        print(f"\n{'='*60}")
        print(f"PROCESSING ARTIST: {artist_name} [{region}]")
        print(f"{'='*60}")

        c = conn.cursor()
        norm_name = artist_name.lower().replace('-', '').replace(' ', '').strip()
        c.execute("SELECT COUNT(*) FROM songs WHERE replace(replace(lower(artist), '-', ''), ' ', '') = ? OR replace(artist_slug, '-', '') = ?", (norm_name, to_slug(artist_name).replace('-', '')))
        existing_cnt = c.fetchone()[0]
        if existing_cnt > 0:
            print(f"[SKIP] Artist {artist_name} already has {existing_cnt} songs in DB.")
            continue

        # 1. Discover GT songs
        gt_url = gt_index.get(artist_name.lower().strip())
        gt_targets = []
        if gt_url:
            print(f"[GT] Discovering songs from {gt_url}...")
            gt_targets = discover_gt_songs(artist_name, gt_url)
            print(f"[GT] Found {len(gt_targets)} song versions.")
        else:
            print(f"[GT] No artist page in index for {artist_name}")

        # 2. Discover UG songs
        print(f"[UG] Discovering songs via search...")
        ug_targets = discover_ug_songs(artist_name)
        print(f"[UG] Found {len(ug_targets)} song versions.")

        # 3. Scrape GT songs
        for item in gt_targets:
            url = item["url"]
            try:
                # Check if already in DB
                c = conn.cursor()
                c.execute("SELECT id FROM songs WHERE source_url = ?", (url,))
                if c.fetchone():
                    continue

                html_content = fetch_html(url)
                if not html_content:
                    continue
                parsed = parse_gt_html(html_content, url, artist_hint=artist_name, title_hint=item["title"])
                if parsed and parsed.content and parsed.chords:
                    c.execute("""
                        INSERT INTO songs (
                            source, source_url, title, artist, title_slug, artist_slug,
                            version_name, rating, votes, key_base, difficulty, category,
                            region, chords, chord_count, line_count, content, content_hash, scraped_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        parsed.source, parsed.source_url, parsed.title, parsed.artist,
                        parsed.title_slug, parsed.artist_slug, item["version_type"],
                        parsed.rating, parsed.votes, parsed.key_base, parsed.difficulty,
                        parsed.category, region, ",".join(parsed.chords),
                        parsed.chord_count, parsed.line_count, parsed.content,
                        parsed.content_hash, parsed.scraped_at
                    ))
                    conn.commit()
                    total_scraped += 1
                    print(f"  + [GT OK] {parsed.artist} - {parsed.title} ({item['version_type']})")
                time.sleep(0.3)
            except Exception as e:
                print(f"  ! [GT ERR] {url}: {e}")

        # 4. Scrape UG songs
        for item in ug_targets:
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
                        INSERT INTO songs (
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
                    total_scraped += 1
                    print(f"  + [UG OK] {parsed.artist} - {parsed.title} ({item['version_type']})")
                time.sleep(0.3)
            except Exception as e:
                print(f"  ! [UG ERR] {url}: {e}")

    conn.close()
    print(f"\nScraping complete! Total songs added: {total_scraped}")


def load_batch_artists(batch_num: int | None = None, batch_file: str | None = None) -> list[tuple[str, str]]:
    """Loads artist name and region from batch CSV."""
    if batch_file:
        csv_path = Path(batch_file)
    elif batch_num:
        csv_path = TOOLS_DIR / "catalog" / "batches" / f"batch_{batch_num:02d}.csv"
    else:
        csv_path = TOOLS_DIR / "catalog" / "batches" / "batch_01.csv"

    if not csv_path.exists():
        raise FileNotFoundError(f"Batch file not found: {csv_path}")

    targets = []
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            art = (row.get("artist") or row.get("artist_name") or "").strip()
            reg = (row.get("region") or row.get("category_region") or "BARAT").strip()
            if art:
                targets.append((art, reg))
    return targets


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="GuitarTuna & Ultimate Guitar Batch Crawler")
    parser.add_argument("--batch", type=int, default=None, help="Batch number to crawl (1 to 15)")
    parser.add_argument("--batch-file", type=str, default=None, help="Path to specific batch CSV")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of artists to crawl in this batch")
    parser.add_argument("--output", type=str, default=None, help="Custom output SQLite database path")
    parser.add_argument("--package", action="store_true", help="Compress result into catalog.db.xz for app assets")
    args = parser.parse_args()

    # Determine targets and output DB
    if args.batch or args.batch_file:
        targets = load_batch_artists(batch_num=args.batch, batch_file=args.batch_file)
        if args.limit:
            targets = targets[:args.limit]
        
        batch_label = f"batch_{args.batch:02d}" if args.batch else Path(args.batch_file).stem
        if args.output:
            db_out = args.output
        else:
            db_dir = TOOLS_DIR / "catalog" / "db_batches"
            db_dir.mkdir(parents=True, exist_ok=True)
            db_out = str(db_dir / f"{batch_label}.db")
        
        print(f"=== Running Crawler for {batch_label} ({len(targets)} artists) -> {db_out} ===")
        crawl_and_build(targets, db_out)
    else:
        # Default test run (Queen, Adele, Incubus, Dewa 19, Sheila on 7)
        TARGETS = [
            ("Queen", "BARAT"),
            ("Adele", "BARAT"),
            ("Incubus", "BARAT"),
            ("Dewa 19", "INDO"),
            ("Sheila on 7", "INDO"),
        ]
        db_out = str(TOOLS_DIR / "catalog" / "batch5_test_catalog.db")
        print(f"=== Running Default Test ({len(TARGETS)} artists) -> {db_out} ===")
        crawl_and_build(TARGETS, db_out)

    if args.package:
        asset_xz = TOOLS_DIR.parent / "app" / "src" / "main" / "assets" / "catalog.db.xz"
        print(f"Compressing {db_out} to {asset_xz}...")
        with open(db_out, "rb") as f_in:
            raw_bytes = f_in.read()
        with lzma.open(asset_xz, "wb") as f_out:
            f_out.write(raw_bytes)
        print("Catalog compressed and updated in assets successfully!")
