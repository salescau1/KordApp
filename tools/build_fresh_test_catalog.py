#!/usr/bin/env python3
"""
Build a fresh test catalog using the new clean crawler pipeline.
Scrapes popular sample songs across GT, UG, and ChordTela.
Compresses output to app/src/main/assets/catalog.db.xz
"""

import lzma
import os
import sqlite3
import sys

# Add project root to sys.path
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

from crawler import fetch_html, parse_song

TARGET_SCHEMA = """
CREATE TABLE songs (
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
CREATE INDEX idx_source ON songs (source);
CREATE INDEX idx_title_slug ON songs (title_slug);
CREATE INDEX idx_artist_slug ON songs (artist_slug);
"""

TARGET_SONGS = [
    # QUEEN
    {
        "source": "GT",
        "url": "https://guitartuna.com/chords/bohemian-rhapsody-queen-easy-guitar-chords-61c7ede2c166d7af0827b771",
        "artist": "Queen",
        "title": "Bohemian Rhapsody",
        "region": "BARAT",
    },
    {
        "source": "UG",
        "url": "https://tabs.ultimate-guitar.com/tab/queen/bohemian-rhapsody-chords-1716013",
        "artist": "Queen",
        "title": "Bohemian Rhapsody",
        "region": "BARAT",
    },
    {
        "source": "GT",
        "url": "https://guitartuna.com/chords/we-are-the-champions-queen-easy-guitar-chords-61321003372a58d4696d0489",
        "artist": "Queen",
        "title": "We Are The Champions",
        "region": "BARAT",
    },
    {
        "source": "UG",
        "url": "https://tabs.ultimate-guitar.com/tab/queen/love-of-my-life-chords-340088",
        "artist": "Queen",
        "title": "Love of My Life",
        "region": "BARAT",
    },

    # ADELE
    {
        "source": "GT",
        "url": "https://guitartuna.com/chords/when-we-were-young-adele-easy-guitar-chords-61097afc1beff32aaef763ea",
        "artist": "Adele",
        "title": "When We Were Young",
        "region": "BARAT",
    },
    {
        "source": "GT",
        "url": "https://guitartuna.com/chords/make-you-feel-my-love-adele-easy-guitar-chords-6109442baddec83eff2e8189",
        "artist": "Adele",
        "title": "Make You Feel My Love",
        "region": "BARAT",
    },
    # ADELE - UG
    {
        "source": "UG",
        "url": "https://tabs.ultimate-guitar.com/tab/adele/someone-like-you-chords-1006751",
        "artist": "Adele",
        "title": "Someone Like You",
        "region": "BARAT",
    },
    {
        "source": "UG",
        "url": "https://tabs.ultimate-guitar.com/tab/adele/make-you-feel-my-love-chords-752102",
        "artist": "Adele",
        "title": "Make You Feel My Love",
        "region": "BARAT",
    },
]


def main():
    print("=" * 60)
    print(f"Scraping {len(TARGET_SONGS)} fresh songs with NEW pipeline...")
    print("=" * 60)

    parsed_rows = []
    for item in TARGET_SONGS:
        print(f"\nFetching [{item['source']}] {item['artist']} - {item['title']}...")
        html = fetch_html(item["url"])
        if not html:
            print(f"  [ERROR] Failed to fetch HTML for {item['url']}")
            continue

        song = parse_song(
            source=item["source"],
            html_text=html,
            url=item["url"],
            artist_hint=item["artist"],
            title_hint=item["title"]
        )
        if not song:
            print(f"  [ERROR] Failed to parse {item['url']}")
            continue

        # Force override display artist/title if hint provided
        if item.get("artist"):
            song.artist = item["artist"]
        if item.get("title"):
            song.title = item["title"]
        if item.get("region"):
            song.region = item["region"]
            song.category = item["region"]

        row = song.to_db_row()
        parsed_rows.append(row)
        print(f"  [SUCCESS] {song.title} ({song.source} - {song.version_name}) | Chords: {len(song.chords)} | Lines: {song.line_count}")

    print("\n" + "=" * 60)
    print(f"Total parsed successfully: {len(parsed_rows)} / {len(TARGET_SONGS)}")
    print("=" * 60)

    # Write SQLite DB
    out_dir = os.path.join(os.path.dirname(__file__), "catalog")
    os.makedirs(out_dir, exist_ok=True)
    db_path = os.path.join(out_dir, "test_fresh_rebuilt.db")
    if os.path.exists(db_path):
        os.remove(db_path)

    conn = sqlite3.connect(db_path)
    conn.executescript(TARGET_SCHEMA)

    cols = list(parsed_rows[0].keys())
    placeholders = ",".join("?" for _ in cols)
    conn.executemany(
        f"INSERT INTO songs ({','.join(cols)}) VALUES ({placeholders})",
        [tuple(r[c] for c in cols) for r in parsed_rows]
    )
    conn.commit()
    conn.close()

    print(f"Saved DB at: {db_path} ({os.path.getsize(db_path)} bytes)")

    # Compress to app/src/main/assets/catalog.db.xz
    xz_path = os.path.abspath(os.path.join(
        os.path.dirname(__file__), "..", "app", "src", "main", "assets", "catalog.db.xz"
    ))
    with open(db_path, "rb") as f_in:
        raw_db_bytes = f_in.read()

    with lzma.open(xz_path, "wb", preset=6) as f_out:
        f_out.write(raw_db_bytes)

    print(f"Compressed fresh catalog to asset: {xz_path} ({os.path.getsize(xz_path)} bytes)")


if __name__ == "__main__":
    main()
