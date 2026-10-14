#!/usr/bin/env python3
"""
harvest_jet.py
Scrapes and integrates the complete discography of Australian rock band "Jet"
from GuitarTuna (GT) and Ultimate Guitar (UG) into full_merged_catalog.db.

Rules:
- GT: Easy Chords (Tier A) & Original Version (Tier B)
- UG: Top Votes (Tier A) & Best Rating (Tier B)
- Metadata: Artist="Jet", Region="BARAT", clean titles & slugs
- Room SQLite integrity: user_version=6, identity_hash='a69e31afaf7d1570cec628f49411725a'
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

TITLE_MAP = {
    "look what youve done": "Look What You've Done",
    "look what you've done": "Look What You've Done",
    "are you gonna be my girl": "Are You Gonna Be My Girl",
    "cold hard bitch": "Cold Hard Bitch",
    "shes a genius": "She's A Genius",
    "she's a genius": "She's A Genius",
    "rollover dj": "Rollover DJ",
    "sgt major": "Sgt. Major",
    "sgt. major": "Sgt. Major",
    "cigarettes and cola": "Cigarettes & Cola",
    "cigarettes cola": "Cigarettes & Cola",
    "cigarettes & cola": "Cigarettes & Cola",
    "thats all lies": "That's All Lies",
    "that's all lies": "That's All Lies",
    "lesprit descalier": "L'esprit D'escalier",
    "l'esprit d'escalier": "L'esprit D'escalier",
    "kia killed in action": "K.I.A. (Killed in Action)",
    "k.i.a. (killed in action)": "K.I.A. (Killed in Action)",
    "i only like you when im high": "I Only Like You When I'm High",
    "i only like you when i'm high": "I Only Like You When I'm High",
    "nothing i cant change demo": "Nothing I Can't Change (Demo)",
    "nothing i can't change (demo)": "Nothing I Can't Change (Demo)",
    "snap your fingers barbados demo": "Snap Your Fingers (Barbados Demo)",
    "snap your fingers (barbados demo)": "Snap Your Fingers (Barbados Demo)",
    "aint that a lotta love": "Ain't That A Lotta Love",
    "ain't that a lotta love": "Ain't That A Lotta Love",
    "thats alright mama": "That's Alright Mama",
    "that's alright mama": "That's Alright Mama",
    "you dont look the same": "You Don't Look The Same",
    "you don't look the same": "You Don't Look The Same",
    "hold on danny elfman and": "Hold On",
    "hey kids": "Hey Kids",
    "last chance": "Last Chance",
    "skin and bones": "Skin And Bones",
    "all you have to do": "All You Have To Do",
    "come on come on": "Come On Come On",
    "shiny magazine": "Shiny Magazine",
    "coming home soon": "Coming Home Soon",
    "stand up": "Stand Up",
    "kings horses": "Kings Horses",
    "lazy gun": "Lazy Gun",
    "beat on repeat": "Beat On Repeat",
    "black hearts on fire": "Black Hearts (On Fire)",
    "black hearts (on fire)": "Black Hearts (On Fire)",
}


def clean_jet_title(raw_title: str) -> str:
    cleaned = clean_text(raw_title).strip()
    low = cleaned.lower()
    if low in TITLE_MAP:
        return TITLE_MAP[low]
    # Handle capitalization if all lower or all upper
    if cleaned.islower() or cleaned.isupper():
        cleaned = cleaned.title()
    return cleaned


def make_clean_slug(text: str) -> str:
    s = text.lower().replace("'", "").replace('"', "")
    s = re.sub(r"[^\w\s-]", "", s)
    s = re.sub(r"[\s_-]+", "-", s)
    return s.strip("-")


def harvest_gt_songs() -> list[dict]:
    print("\n--- Harvesting GuitarTuna (GT) for Jet ---")
    artist_url = "https://guitartuna.com/artists/jet-easy-original-guitar-chords-tabs-619e284d1d5ae0912b811329"
    html_text = fetch_html(artist_url)
    if not html_text:
        print("Failed to fetch GT artist page!")
        return []

    soup = BeautifulSoup(html_text, "html.parser")
    artist_slug = "jet"
    hex_groups: dict[str, dict[str, dict[str, str]]] = defaultdict(lambda: defaultdict(dict))

    for a in soup.find_all("a", href=True):
        href = a["href"]
        if "/chords/" not in href or f"-{artist_slug}-" not in href:
            continue
        m = re.search(rf"/chords/(.*?)-{artist_slug}-(easy|original)-guitar-chords-([0-9a-fA-F]+)", href)
        if m:
            song_key = m.group(1).lower().strip()
            v_type = m.group(2)  # easy or original
            song_hex = m.group(3).lower().strip()
            full_url = "https://guitartuna.com" + href if href.startswith("/") else href
            hex_groups[song_key][song_hex][v_type] = full_url

    candidate_items = []
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
            candidate_items.append({
                "source": "GT",
                "title_guess": title_guess,
                "url": vmap["easy"],
                "version_name": "Easy Chords",
                "tier": "A",
            })
        if "original" in vmap:
            candidate_items.append({
                "source": "GT",
                "title_guess": title_guess,
                "url": vmap["original"],
                "version_name": "Original Version",
                "tier": "B",
            })

    print(f"Found {len(candidate_items)} GT song items to process.")
    results = []
    for item in candidate_items:
        url = item["url"]
        page_html = fetch_html(url)
        if not page_html:
            print(f"  [GT] Fetch failed: {url}")
            continue
        parsed = parse_gt_html(page_html, url, artist_hint="Jet", title_hint=item["title_guess"])
        if not parsed:
            print(f"  [GT] Parse failed: {url}")
            continue

        raw_title = parsed.title if parsed.title and parsed.title != "Unknown Title" else item["title_guess"]
        final_title = clean_jet_title(raw_title)
        title_slug = make_clean_slug(final_title)
        chords_str = ",".join(parsed.chords)
        content_hash = hashlib.sha256(parsed.content.encode("utf-8")).hexdigest()
        scraped_at = datetime.now(timezone.utc).isoformat()

        row = {
            "source": "GT",
            "source_url": url,
            "title": final_title,
            "artist": "Jet",
            "title_slug": title_slug,
            "artist_slug": "jet",
            "version_name": item["version_name"],
            "rating": None,
            "votes": None,
            "key_base": None,
            "difficulty": "Beginner" if item["version_name"] == "Easy Chords" else "Intermediate",
            "category": "Rock",
            "region": "BARAT",
            "chords": chords_str,
            "chord_count": parsed.chord_count,
            "line_count": parsed.line_count,
            "content": parsed.content,
            "content_hash": content_hash,
            "scraped_at": scraped_at,
            "tier": item["tier"],
        }
        results.append(row)
        print(f"  [GT] OK: {final_title} ({item['version_name']}) -> Tier {item['tier']}")
        time.sleep(0.05)

    return results


def harvest_ug_songs() -> list[dict]:
    print("\n--- Harvesting Ultimate Guitar (UG) for Jet ---")
    encoded_artist = quote("Jet")
    tabs = []
    current_page = 1
    total_pages = 1

    while current_page <= total_pages and current_page <= 10:
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
            if item.get("type") == "Chords":
                art = (item.get("artist_name") or "").strip()
                if art.lower() == "jet":
                    tabs.append(item)
        current_page += 1

    print(f"Total UG Chords tabs for exact artist 'Jet': {len(tabs)}")
    by_song = defaultdict(list)
    for t in tabs:
        song_name = (t.get("song_name") or "").strip()
        if song_name:
            by_song[song_name.lower()].append(t)

    selected = []
    for s_name, tab_list in sorted(by_song.items()):
        # 1. Top votes -> Tier A
        sorted_by_votes = sorted(tab_list, key=lambda x: int(x.get("votes") or 0), reverse=True)
        top_votes = sorted_by_votes[0]
        v1_num = top_votes.get("version", 1)
        selected.append((top_votes, f"Ver {v1_num} (Top Votes)", "A"))

        # 2. Best rating -> Tier B
        remaining = [x for x in tab_list if x.get("tab_url") != top_votes.get("tab_url")]
        if remaining:
            qualified = [x for x in remaining if int(x.get("votes") or 0) >= 3]
            candidates = qualified if qualified else remaining
            sorted_by_rating = sorted(candidates, key=lambda x: (float(x.get("rating") or 0.0), int(x.get("votes") or 0)), reverse=True)
            best_rating = sorted_by_rating[0]
            v2_num = best_rating.get("version", 2)
            selected.append((best_rating, f"Ver {v2_num} (Best Rating)", "B"))

    print(f"Selected {len(selected)} UG tabs according to 2-version rule.")
    results = []
    for tab_info, version_name, tier in selected:
        url = tab_info.get("tab_url")
        raw_song_name = tab_info.get("song_name")
        page_html = fetch_html(url)
        if not page_html:
            print(f"  [UG] Fetch failed: {url}")
            continue
        parsed = parse_ug_html(page_html, url, artist_hint="Jet", title_hint=raw_song_name)
        if not parsed:
            print(f"  [UG] Parse failed: {url}")
            continue

        final_title = clean_jet_title(parsed.title or raw_song_name)
        title_slug = make_clean_slug(final_title)
        chords_str = ",".join(parsed.chords)
        content_hash = hashlib.sha256(parsed.content.encode("utf-8")).hexdigest()
        scraped_at = datetime.now(timezone.utc).isoformat()

        row = {
            "source": "UG",
            "source_url": url,
            "title": final_title,
            "artist": "Jet",
            "title_slug": title_slug,
            "artist_slug": "jet",
            "version_name": version_name,
            "rating": parsed.rating,
            "votes": parsed.votes,
            "key_base": parsed.key_base,
            "difficulty": parsed.difficulty,
            "category": "Rock",
            "region": "BARAT",
            "chords": chords_str,
            "chord_count": parsed.chord_count,
            "line_count": parsed.line_count,
            "content": parsed.content,
            "content_hash": content_hash,
            "scraped_at": scraped_at,
            "tier": tier,
        }
        results.append(row)
        print(f"  [UG] OK: {final_title} ({version_name}) [votes: {parsed.votes}, rating: {parsed.rating}] -> Tier {tier}")
        time.sleep(0.05)

    return results


def insert_to_db(rows: list[dict]):
    print(f"\n--- Inserting {len(rows)} Jet songs into {DB_PATH} ---")
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()

    # Verify user_version and identity_hash before insertion
    ver = c.execute("PRAGMA user_version").fetchone()[0]
    id_hash = c.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]
    print(f"Initial DB State: user_version = {ver}, identity_hash = '{id_hash}'")

    inserted = 0
    skipped = 0
    for r in rows:
        existing = c.execute("SELECT id FROM songs WHERE source_url = ?", (r["source_url"],)).fetchone()
        if existing:
            skipped += 1
            continue

        c.execute("""
            INSERT INTO songs (
                source, source_url, title, artist, title_slug, artist_slug,
                version_name, rating, votes, key_base, difficulty, category,
                region, chords, chord_count, line_count, content, content_hash, scraped_at, tier
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            r["source"], r["source_url"], r["title"], r["artist"], r["title_slug"], r["artist_slug"],
            r["version_name"], r["rating"], r["votes"], r["key_base"], r["difficulty"], r["category"],
            r["region"], r["chords"], r["chord_count"], r["line_count"], r["content"], r["content_hash"],
            r["scraped_at"], r["tier"]
        ))
        inserted += 1

    # Ensure Room SQLite integrity is strictly maintained
    c.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
    c.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'a69e31afaf7d1570cec628f49411725a')")
    c.execute("PRAGMA user_version = 6")
    conn.commit()

    total_songs = c.execute("SELECT count(*) FROM songs").fetchone()[0]
    jet_songs = c.execute("SELECT count(*) FROM songs WHERE artist_slug = 'jet'").fetchone()[0]
    final_ver = c.execute("PRAGMA user_version").fetchone()[0]
    final_hash = c.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]

    print(f"\nInsertion Results:")
    print(f"  Inserted: {inserted}")
    print(f"  Skipped (existing): {skipped}")
    print(f"  Total Jet songs in DB: {jet_songs}")
    print(f"  Total songs in DB: {total_songs}")
    print(f"  DB user_version: {final_ver}")
    print(f"  Room identity_hash: '{final_hash}'")

    conn.close()


def main():
    gt_songs = harvest_gt_songs()
    ug_songs = harvest_ug_songs()
    all_songs = gt_songs + ug_songs
    print(f"\nTotal songs harvested: {len(all_songs)} (GT: {len(gt_songs)}, UG: {len(ug_songs)})")
    insert_to_db(all_songs)


if __name__ == "__main__":
    main()
