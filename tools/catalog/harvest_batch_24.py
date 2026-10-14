#!/usr/bin/env python3
"""
harvest_batch_24.py
Harvests and integrates 20 curated artists into KordApp master database:
1. The Vines (BARAT)
2. The Hives (BARAT)
3. The Fratellis (BARAT)
4. Wolfmother (BARAT)
5. The Darkness (BARAT)
6. The Subways (BARAT)
7. Caesars (BARAT)
8. Mando Diao (BARAT)
9. Matchbox 20 (BARAT)
10. Velvet Revolver (BARAT)
11. Theory of a Deadman (BARAT)
12. A Rocket to the Moon (BARAT)
13. All Time Low (BARAT)
14. Bowling for Soup (BARAT)
15. Maneskin (BARAT)
16. Royal Blood (BARAT)
17. Greta Van Fleet (BARAT)
18. Dere (INDO)
19. Danilla (INDO)
20. Gilga Sahid (INDO)

Rules:
- GT: Easy Chords (Tier A) & Original Version (Tier B)
- UG: Top Votes (Tier A/C) & Best Rating (Tier B/C)
- Indo artists: 100% in Tier A or B (0 in Tier C)
- Room SQLite integrity: user_version = 6, identity_hash = 'a69e31afaf7d1570cec628f49411725a'
"""
from __future__ import annotations

import html
import hashlib
import json
import os
import re
import sqlite3
import sys
import time
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import quote
from bs4 import BeautifulSoup

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.fetcher import fetch_html
from crawler.parsers.guitartuna import parse_gt_html
from crawler.parsers.ultimate_guitar import parse_ug_html
from crawler.normalizer import clean_text, ensure_chord_spacing, extract_unique_chords, normalize_sheet

DB_PATH = TOOLS_DIR / "catalog" / "full_merged_catalog.db"
CHORDTELA_DB = Path(r"C:\KordApp-AGY\Catalog\chordtela.db")

TARGET_ARTISTS = [
    {
        "name": "The Vines",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/the-vines-easy-original-guitar-chords-tabs-6374fd1a72dbfaef74894ea9",
        "ug_query": "The Vines",
        "ug_match": ["the vines"],
    },
    {
        "name": "The Hives",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/the-hives-easy-original-guitar-chords-tabs-6374fd0d72dbfaef74894e2a",
        "ug_query": "The Hives",
        "ug_match": ["the hives"],
    },
    {
        "name": "The Fratellis",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/the-fratellis-easy-original-guitar-chords-tabs-618bd539876aec4bed39c9e4",
        "ug_query": "The Fratellis",
        "ug_match": ["the fratellis"],
    },
    {
        "name": "Wolfmother",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/wolfmother-easy-original-guitar-chords-tabs-6374fd2d72dbfaef74894f5e",
        "ug_query": "Wolfmother",
        "ug_match": ["wolfmother"],
    },
    {
        "name": "The Darkness",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/the-darkness-easy-original-guitar-chords-tabs-6374fd0872dbfaef74894dff",
        "ug_query": "The Darkness",
        "ug_match": ["the darkness"],
    },
    {
        "name": "The Subways",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/the-subways-easy-original-guitar-chords-tabs-6374fd1772dbfaef74894e92",
        "ug_query": "The Subways",
        "ug_match": ["the subways"],
    },
    {
        "name": "Caesars",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/caesars-easy-original-guitar-chords-tabs-6374f3e1a60d78bb71b5cdb4",
        "ug_query": "Caesars",
        "ug_match": ["caesars", "caesars palace", "caesars (caesars palace)"],
    },
    {
        "name": "Mando Diao",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/mando-diao-easy-original-guitar-chords-tabs-6697c7e98b88ae8969a7d69d",
        "ug_query": "Mando Diao",
        "ug_match": ["mando diao"],
    },
    {
        "name": "Matchbox 20",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/matchbox-twenty-easy-original-guitar-chords-tabs-61a01d0c8ad4f8fa4a670a14",
        "ug_query": "Matchbox 20",
        "ug_match": ["matchbox 20", "matchbox twenty"],
    },
    {
        "name": "Velvet Revolver",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/velvet-revolver-easy-original-guitar-chords-tabs-6374fd2672dbfaef74894f20",
        "ug_query": "Velvet Revolver",
        "ug_match": ["velvet revolver"],
    },
    {
        "name": "Theory of a Deadman",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/theory-of-a-deadman-easy-original-guitar-chords-tabs-6374fd1c72dbfaef74894ebb",
        "ug_query": "Theory of a Deadman",
        "ug_match": ["theory of a deadman"],
    },
    {
        "name": "A Rocket to the Moon",
        "region": "BARAT",
        "category": "Pop/Rock",
        "gt_url": "https://guitartuna.com/artists/a-rocket-to-the-moon-easy-original-guitar-chords-tabs-6374e616a60d78bb71b5c1fe",
        "ug_query": "A Rocket to the Moon",
        "ug_match": ["a rocket to the moon"],
    },
    {
        "name": "All Time Low",
        "region": "BARAT",
        "category": "Pop/Punk",
        "gt_url": "https://guitartuna.com/artists/all-time-low-easy-original-guitar-chords-tabs-6374e616a60d78bb71b5c241",
        "ug_query": "All Time Low",
        "ug_match": ["all time low"],
    },
    {
        "name": "Bowling for Soup",
        "region": "BARAT",
        "category": "Pop/Punk",
        "gt_url": "https://guitartuna.com/artists/bowling-for-soup-easy-original-guitar-chords-tabs-6374f3e1a60d78bb71b5cd79",
        "ug_query": "Bowling for Soup",
        "ug_match": ["bowling for soup"],
    },
    {
        "name": "Maneskin",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/maneskin-easy-original-guitar-chords-tabs-6374fce872dbfaef74894de2",
        "ug_query": "Maneskin",
        "ug_match": ["maneskin", "måneskin"],
    },
    {
        "name": "Royal Blood",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/royal-blood-easy-original-guitar-chords-tabs-6374f424a60d78bb71b5d345",
        "ug_query": "Royal Blood",
        "ug_match": ["royal blood"],
    },
    {
        "name": "Greta Van Fleet",
        "region": "BARAT",
        "category": "Rock",
        "gt_url": "https://guitartuna.com/artists/greta-van-fleet-easy-original-guitar-chords-tabs-61a89a59c3c263e4d37efacc",
        "ug_query": "Greta Van Fleet",
        "ug_match": ["greta van fleet"],
    },
    {
        "name": "Dere",
        "region": "INDO",
        "category": "Indie",
        "gt_url": "https://guitartuna.com/artists/dere-easy-original-guitar-chords-tabs-66e200a7f7abd9b95a896e82",
        "ug_query": "Dere",
        "ug_match": ["dere"],
    },
    {
        "name": "Danilla",
        "region": "INDO",
        "category": "Indie/Jazz",
        "gt_url": "https://guitartuna.com/artists/danilla-easy-original-guitar-chords-tabs-67bdaa2d30dfa087b47961db",
        "ug_query": "Danilla",
        "ug_match": ["danilla", "danilla riyadi"],
    },
    {
        "name": "Gilga Sahid",
        "region": "INDO",
        "category": "Dangdut/Pop",
        "gt_url": None,
        "ug_query": "Gilga Sahid",
        "ug_match": ["gilga sahid", "gildcoustic"],
    },
]


def make_clean_slug(text: str) -> str:
    s = text.lower().replace("'", "").replace('"', "")
    s = re.sub(r"[^\w\s-]", "", s)
    s = re.sub(r"[\s_-]+", "-", s)
    return s.strip("-")


def format_title_and_feat(raw_artist: str, raw_title: str, target_artist: str) -> tuple[str, str]:
    art = raw_artist.strip()
    tit = clean_text(raw_title).strip()
    
    # Capitalize title if entirely lowercase or uppercase
    if tit.islower() or (tit.isupper() and len(tit) > 4):
        tit = tit.title()

    # Check feat in title
    m_feat_title = re.search(r'\((?:feat\.?|ft\.?)\s+([^)]+)\)', tit, re.I)
    
    feat_partner = None
    m_art_feat = re.search(r'^(.*?)\s+(?:feat\.?|ft\.?|featuring)\s+(.*)$', art, re.I)
    if m_art_feat:
        p1 = m_art_feat.group(1).strip()
        p2 = m_art_feat.group(2).strip()
        feat_partner = p2 if target_artist.lower() in p1.lower() else p1
    elif " with " in art.lower():
        parts = re.split(r'\s+with\s+', art, flags=re.I)
        feat_partner = parts[1].strip() if target_artist.lower() in parts[0].lower() else parts[0].strip()
    elif ", " in art:
        parts = [p.strip() for p in art.split(",")]
        others = [p for p in parts if target_artist.lower() not in p.lower()]
        if others:
            feat_partner = ", ".join(others)

    if feat_partner and not m_feat_title:
        # Avoid duplicating feat
        tit = f"{tit} (feat. {feat_partner})"

    return target_artist, tit


def harvest_gt(artist_cfg: dict) -> list[dict]:
    gt_url = artist_cfg.get("gt_url")
    if not gt_url:
        return []

    target_name = artist_cfg["name"]
    region = artist_cfg["region"]
    category = artist_cfg["category"]

    html_text = fetch_html(gt_url)
    if not html_text:
        return []

    soup = BeautifulSoup(html_text, "html.parser")
    m_slug = re.search(r"/artists/([a-zA-Z0-9_-]+?)-easy-original-guitar-chords-tabs", gt_url)
    artist_slug_in_url = m_slug.group(1).lower().strip() if m_slug else make_clean_slug(target_name)

    hex_groups: dict[str, dict[str, dict[str, str]]] = defaultdict(lambda: defaultdict(dict))

    for a in soup.find_all("a", href=True):
        href = a["href"]
        if "/chords/" not in href:
            continue
        # Check if artist slug in href
        if f"-{artist_slug_in_url}-" in href or f"-{make_clean_slug(target_name)}-" in href:
            m = re.search(r"/chords/(.*?)-(?:[a-zA-Z0-9_-]+)-(easy|original)-guitar-chords-([0-9a-fA-F]+)", href)
            if m:
                song_key = m.group(1).lower().strip()
                v_type = m.group(2)
                song_hex = m.group(3).lower().strip()
                full_url = "https://guitartuna.com" + href if href.startswith("/") else href
                hex_groups[song_key][song_hex][v_type] = full_url

    items = []
    for song_key, hex_map in hex_groups.items():
        title_guess = song_key.replace("-", " ")
        best_hex = None
        for s_hex, vmap in hex_map.items():
            if "easy" in vmap and "original" in vmap:
                best_hex = s_hex
                break
        if not best_hex:
            best_hex = next(iter(hex_map.keys()))

        vmap = hex_map[best_hex]
        if "easy" in vmap:
            items.append((title_guess, vmap["easy"], "Easy Chords", "A"))
        if "original" in vmap:
            items.append((title_guess, vmap["original"], "Original Version", "B"))

    results = []
    for title_guess, url, v_name, tier in items:
        page_html = fetch_html(url)
        if not page_html:
            continue
        parsed = parse_gt_html(page_html, url, artist_hint=target_name, title_hint=title_guess)
        if not parsed:
            continue

        raw_title = parsed.title if parsed.title and parsed.title != "Unknown Title" else title_guess
        final_art, final_title = format_title_and_feat(parsed.artist, raw_title, target_name)
        title_slug = make_clean_slug(final_title)
        artist_slug = make_clean_slug(final_art)
        chords_str = ",".join(parsed.chords)
        content_hash = hashlib.sha256(parsed.content.encode("utf-8")).hexdigest()
        scraped_at = datetime.now(timezone.utc).isoformat()

        row = {
            "source": "GT",
            "source_url": url,
            "title": final_title,
            "artist": final_art,
            "title_slug": title_slug,
            "artist_slug": artist_slug,
            "version_name": v_name,
            "rating": None,
            "votes": None,
            "key_base": None,
            "difficulty": "Beginner" if v_name == "Easy Chords" else "Intermediate",
            "category": category,
            "region": region,
            "chords": chords_str,
            "chord_count": parsed.chord_count,
            "line_count": parsed.line_count,
            "content": parsed.content,
            "content_hash": content_hash,
            "scraped_at": scraped_at,
            "tier": tier,
        }
        results.append(row)
        time.sleep(0.04)

    return results


def harvest_ug(artist_cfg: dict) -> list[dict]:
    target_name = artist_cfg["name"]
    region = artist_cfg["region"]
    category = artist_cfg["category"]
    ug_query = artist_cfg["ug_query"]
    ug_matches = [m.lower() for m in artist_cfg["ug_match"]]

    tabs = []
    for page in range(1, 8):
        url = f"https://www.ultimate-guitar.com/search.php?title={quote(ug_query)}&type=300&page={page}"
        html_text = fetch_html(url)
        if not html_text:
            break
        m = re.search(r'class="js-store"\s+data-content="([^"]+)"', html_text)
        if not m:
            break
        try:
            data = json.loads(html.unescape(m.group(1)))
        except Exception:
            break
        results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
        for r in results:
            if r.get('type') == 'Chords':
                art = (r.get('artist_name') or '').strip().lower()
                if art in ug_matches:
                    tabs.append(r)
        if not data.get('store', {}).get('page', {}).get('data', {}).get('pagination', {}).get('next'):
            break

    by_song = defaultdict(list)
    for t in tabs:
        sn = (t.get('song_name') or '').strip().lower()
        if sn:
            by_song[sn].append(t)

    selected = []
    for sn, tab_list in sorted(by_song.items()):
        # 1. Top Votes
        sorted_by_votes = sorted(tab_list, key=lambda x: int(x.get('votes') or 0), reverse=True)
        top_votes = sorted_by_votes[0]
        v1_votes = int(top_votes.get('votes') or 0)
        v1_num = top_votes.get('version', 1)

        # Tier assignment
        if region == "INDO":
            tier_top = "A"
        else:
            tier_top = "A" if v1_votes >= 5 else "C"

        selected.append((top_votes, f"Ver {v1_num} (Top Votes)", tier_top))

        # 2. Best Rating
        remaining = [x for x in tab_list if x.get('tab_url') != top_votes.get('tab_url')]
        if remaining:
            qualified = [x for x in remaining if int(x.get('votes') or 0) >= 3]
            candidates = qualified if qualified else remaining
            sorted_by_rating = sorted(candidates, key=lambda x: (float(x.get('rating') or 0.0), int(x.get('votes') or 0)), reverse=True)
            best_rating = sorted_by_rating[0]
            v2_votes = int(best_rating.get('votes') or 0)
            v2_num = best_rating.get('version', 2)

            if region == "INDO":
                tier_best = "B"
            else:
                tier_best = "B" if v2_votes >= 5 else "C"

            selected.append((best_rating, f"Ver {v2_num} (Best Rating)", tier_best))

    results = []
    for tab_info, version_name, tier in selected:
        url = tab_info.get("tab_url")
        raw_song_name = tab_info.get("song_name")
        raw_artist = tab_info.get("artist_name")

        page_html = fetch_html(url)
        if not page_html:
            continue
        parsed = parse_ug_html(page_html, url, artist_hint=target_name, title_hint=raw_song_name)
        if not parsed:
            continue

        raw_title = parsed.title or raw_song_name
        final_art, final_title = format_title_and_feat(raw_artist or target_name, raw_title, target_name)
        title_slug = make_clean_slug(final_title)
        artist_slug = make_clean_slug(final_art)
        chords_str = ",".join(parsed.chords)
        content_hash = hashlib.sha256(parsed.content.encode("utf-8")).hexdigest()
        scraped_at = datetime.now(timezone.utc).isoformat()

        row = {
            "source": "UG",
            "source_url": url,
            "title": final_title,
            "artist": final_art,
            "title_slug": title_slug,
            "artist_slug": artist_slug,
            "version_name": version_name,
            "rating": parsed.rating,
            "votes": parsed.votes,
            "key_base": parsed.key_base,
            "difficulty": parsed.difficulty,
            "category": category,
            "region": region,
            "chords": chords_str,
            "chord_count": parsed.chord_count,
            "line_count": parsed.line_count,
            "content": parsed.content,
            "content_hash": content_hash,
            "scraped_at": scraped_at,
            "tier": tier,
        }
        results.append(row)
        time.sleep(0.04)

    return results


def harvest_chordtela_indo(artist_cfg: dict) -> list[dict]:
    target_name = artist_cfg["name"]
    if artist_cfg["region"] != "INDO" or not CHORDTELA_DB.exists():
        return []

    conn = sqlite3.connect(CHORDTELA_DB)
    c = conn.cursor()
    c.execute("""
        SELECT artist, title, source_url, chords, chord_count, line_count, content, scraped_at
        FROM songs
        WHERE lower(artist) LIKE ?
    """, (f"%{target_name.lower()}%",))
    rows = c.fetchall()
    conn.close()

    results = []
    for r in rows:
        raw_art, raw_tit, url, chords, chord_cnt, line_cnt, content, sc_at = r
        # Filter out unrelated artists (like Cinderella for Dere)
        if target_name.lower() == "dere" and "dere" not in raw_art.lower().split():
            if raw_art.lower() != "dere":
                continue

        final_art, final_title = format_title_and_feat(raw_art, raw_tit, target_name)
        title_slug = make_clean_slug(final_title)
        artist_slug = make_clean_slug(final_art)

        # Clean chords format (ensure comma-separated)
        clean_chords = [c.strip() for c in re.split(r'[, ]+', chords) if c.strip()]
        chords_str = ",".join(clean_chords)
        content_hash = hashlib.sha256(content.encode("utf-8")).hexdigest()

        row = {
            "source": "ChordTela",
            "source_url": url,
            "title": final_title,
            "artist": final_art,
            "title_slug": title_slug,
            "artist_slug": artist_slug,
            "version_name": "ChordTela",
            "rating": None,
            "votes": None,
            "key_base": None,
            "difficulty": "Beginner",
            "category": artist_cfg["category"],
            "region": "INDO",
            "chords": chords_str,
            "chord_count": len(clean_chords),
            "line_count": line_cnt,
            "content": content,
            "content_hash": content_hash,
            "scraped_at": sc_at or datetime.now(timezone.utc).isoformat(),
            "tier": "A",
        }
        results.append(row)

    return results


def process_batch_24():
    print(f"=== HARVESTING BATCH 24 (20 ARTISTS) ===")
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()

    existing_urls = set(r[0] for r in c.execute("SELECT source_url FROM songs").fetchall())
    print(f"Initial songs in master DB: {len(existing_urls):,}")

    summary_per_artist = []

    for idx, artist_cfg in enumerate(TARGET_ARTISTS, 1):
        name = artist_cfg["name"]
        print(f"\n[{idx}/20] Processing {name} ({artist_cfg['region']})...")

        gt_songs = harvest_gt(artist_cfg)
        ug_songs = harvest_ug(artist_cfg)
        ct_songs = harvest_chordtela_indo(artist_cfg)

        combined = gt_songs + ug_songs + ct_songs
        new_songs = []
        for s in combined:
            if s["source_url"] not in existing_urls:
                existing_urls.add(s["source_url"])
                new_songs.append(s)

        inserted_count = 0
        tier_counts = defaultdict(int)
        for s in new_songs:
            c.execute("""
                INSERT INTO songs (
                    source, source_url, title, artist, title_slug, artist_slug,
                    version_name, rating, votes, key_base, difficulty, category,
                    region, chords, chord_count, line_count, content, content_hash, scraped_at, tier
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                s["source"], s["source_url"], s["title"], s["artist"], s["title_slug"], s["artist_slug"],
                s["version_name"], s["rating"], s["votes"], s["key_base"], s["difficulty"], s["category"],
                s["region"], s["chords"], s["chord_count"], s["line_count"], s["content"], s["content_hash"],
                s["scraped_at"], s["tier"]
            ))
            inserted_count += 1
            tier_counts[s["tier"]] += 1

        conn.commit()

        # Query total songs for this artist now in DB
        a_slug = make_clean_slug(name)
        total_for_art = c.execute("SELECT count(*) FROM songs WHERE artist_slug = ? OR lower(artist) = ?", (a_slug, name.lower())).fetchone()[0]
        unique_titles = c.execute("SELECT count(DISTINCT title_slug) FROM songs WHERE artist_slug = ? OR lower(artist) = ?", (a_slug, name.lower())).fetchone()[0]

        summary = {
            "name": name,
            "region": artist_cfg["region"],
            "gt_harvested": len(gt_songs),
            "ug_harvested": len(ug_songs),
            "ct_harvested": len(ct_songs),
            "inserted": inserted_count,
            "tiers": dict(tier_counts),
            "total_in_db": total_for_art,
            "unique_titles": unique_titles,
        }
        summary_per_artist.append(summary)
        print(f"  + Added {inserted_count} new songs (A:{tier_counts['A']}, B:{tier_counts['B']}, C:{tier_counts['C']}) | Total in DB: {total_for_art} ({unique_titles} unique titles)")

    print("\n--- Standardizing Artist Names & Indo Tier Guarantees ---")
    # 1. Standardize Danilla Riyadi -> Danilla
    c.execute("""
        UPDATE songs 
        SET artist = 'Danilla', artist_slug = 'danilla', region = 'INDO'
        WHERE lower(artist) IN ('danilla riyadi', 'danilla')
    """)
    # 2. Standardize Matchbox Twenty -> Matchbox 20
    c.execute("""
        UPDATE songs 
        SET artist = 'Matchbox 20', artist_slug = 'matchbox-20', region = 'BARAT'
        WHERE lower(artist) = 'matchbox twenty'
    """)
    # 3. Standardize Måneskin -> Maneskin
    c.execute("""
        UPDATE songs 
        SET artist = 'Maneskin', artist_slug = 'maneskin', region = 'BARAT'
        WHERE lower(artist) = 'måneskin'
    """)
    # 4. Guarantee Indonesian songs NEVER in Tier C
    c.execute("""
        UPDATE songs 
        SET tier = 'B' 
        WHERE region = 'INDO' AND tier = 'C'
    """)

    # 5. Room SQLite Integrity
    c.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
    c.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'a69e31afaf7d1570cec628f49411725a')")
    c.execute("PRAGMA user_version = 6")
    conn.commit()

    total_final = c.execute("SELECT count(*) FROM songs").fetchone()[0]
    user_ver = c.execute("PRAGMA user_version").fetchone()[0]
    id_hash = c.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]

    print(f"\nFinal Master DB status: {total_final:,} songs, user_version={user_ver}, identity_hash='{id_hash}'")
    conn.close()

    # Save summary json
    summary_path = TOOLS_DIR / "catalog" / "batch_24_summary.json"
    with open(summary_path, "w", encoding="utf-8") as f:
        json.dump(summary_per_artist, f, indent=2)
    print(f"Summary written to {summary_path}")

    return summary_per_artist


if __name__ == "__main__":
    process_batch_24()
