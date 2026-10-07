import sys
import os
import urllib.request
import re
import json
import html
import hashlib
import sqlite3
import lzma

sys.path.insert(0, r"C:\ChordApp")
from scraper import gt_scraper, ug_scraper, normalize

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

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9',
}

TARGETS = [
    # ADELE - UG
    {"source": "UG", "artist": "Adele", "title": "Someone Like You", "artist_slug": "adele", "title_slug": "someone-like-you", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/adele/someone-like-you-chords-1006751"},
    {"source": "UG", "artist": "Adele", "title": "Make You Feel My Love", "artist_slug": "adele", "title_slug": "make-you-feel-my-love", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/adele/make-you-feel-my-love-chords-752102"},
    # QUEEN - UG
    {"source": "UG", "artist": "Queen", "title": "Bohemian Rhapsody", "artist_slug": "queen", "title_slug": "bohemian-rhapsody", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/queen/bohemian-rhapsody-chords-1716013"},
    {"source": "UG", "artist": "Queen", "title": "Love of My Life", "artist_slug": "queen", "title_slug": "love-of-my-life", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/queen/love-of-my-life-chords-340088"},
    {"source": "UG", "artist": "Queen", "title": "Crazy Little Thing Called Love", "artist_slug": "queen", "title_slug": "crazy-little-thing-called-love", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/queen/crazy-little-thing-called-love-chords-974560"},
    
    # ADELE - GT
    {"source": "GT", "artist": "Adele", "title": "Make You Feel My Love", "artist_slug": "adele", "title_slug": "make-you-feel-my-love", "version": "Easy Chords", "url": "https://guitartuna.com/chords/make-you-feel-my-love-adele-easy-guitar-chords-6109442baddec83eff2e8189"},
    {"source": "GT", "artist": "Adele", "title": "When We Were Young", "artist_slug": "adele", "title_slug": "when-we-were-young", "version": "Easy Chords", "url": "https://guitartuna.com/chords/when-we-were-young-adele-easy-guitar-chords-61097afc1beff32aaef763ea"},
    # QUEEN - GT
    {"source": "GT", "artist": "Queen", "title": "Bohemian Rhapsody", "artist_slug": "queen", "title_slug": "bohemian-rhapsody", "version": "Easy Chords", "url": "https://guitartuna.com/chords/bohemian-rhapsody-queen-easy-guitar-chords-61c7ede2c166d7af0827b771"},
    {"source": "GT", "artist": "Queen", "title": "Bohemian Rhapsody", "artist_slug": "queen", "title_slug": "bohemian-rhapsody", "version": "Original Chords", "url": "https://guitartuna.com/chords/bohemian-rhapsody-queen-original-guitar-chords-61c7ede2c166d7af0827b771"},
    {"source": "GT", "artist": "Queen", "title": "Crazy Little Thing Called Love", "artist_slug": "queen", "title_slug": "crazy-little-thing-called-love", "version": "Easy Chords", "url": "https://guitartuna.com/chords/crazy-little-thing-called-love-queen-easy-guitar-chords-60c094a7dd40eea46fb92482"},
]

def main():
    rows = []
    print(f"Scraping fresh {len(TARGETS)} songs directly from web...")
    
    for t in TARGETS:
        url = t["url"]
        src = t["source"]
        print(f"--> [{src}] Fetching {t['artist']} - {t['title']} ({url})...")
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=15) as resp:
                raw_html = resp.read().decode('utf-8', errors='ignore')
        except Exception as e:
            print(f"    FAILED fetch {url}: {e}")
            continue

        if src == "UG":
            store = ug_scraper.parse_js_store(raw_html)
            pdata = ug_scraper.page_data(store)
            tab_data = pdata.get("tab_view", {})
            tab_info = pdata.get("tab", {})
            raw_content = tab_data.get("wiki_tab", {}).get("content", "")
            if not raw_content:
                print("    Empty content from UG js-store")
                continue
            # Transform [ch]...[/ch] to [...]
            content = re.sub(r'\[ch\](.*?)\[/ch\]', r'[\1]', raw_content)
            chords_set = sorted(list(set(re.findall(r'\[([^\]]+)\]', content))))
            key_base = tab_data.get("meta", {}).get("tonality") or ""
            rating = float(tab_info.get("rating") or 0.0)
            votes = int(tab_info.get("votes") or 0)
            difficulty = tab_data.get("meta", {}).get("difficulty") or "Intermediate"
        elif src == "GT":
            content, lyric_lines = gt_scraper.build_sheet(raw_html)
            if not content:
                print("    Empty content from GT RSC")
                continue
            chords_set = sorted(list(set(re.findall(r'\[([^\]]+)\]', content))))
            key_base = ""
            rating = 5.0
            votes = 100
            difficulty = "Beginner"
        else:
            continue

        chords_json = json.dumps(chords_set)
        chord_count = len(chords_set)
        line_count = len(content.splitlines())
        content_hash = hashlib.sha256(content.encode("utf-8")).hexdigest()
        scraped_at = "2026-10-03T15:15:00Z"

        rows.append({
            "source": src,
            "source_url": url,
            "title": t["title"],
            "artist": t["artist"],
            "title_slug": t["title_slug"],
            "artist_slug": t["artist_slug"],
            "version_name": t["version"],
            "rating": rating,
            "votes": votes,
            "key_base": key_base,
            "difficulty": difficulty,
            "category": "BARAT",
            "region": "BARAT",
            "chords": chords_json,
            "chord_count": chord_count,
            "line_count": line_count,
            "content": content,
            "content_hash": content_hash,
            "scraped_at": scraped_at,
        })
        print(f"    [OK] Extracted {t['title']} ({src}): {chord_count} chords, {line_count} lines")

    # Build DB
    db_path = r"C:\KordApp-AGY\tools\catalog\fresh_test_catalog.db"
    if os.path.exists(db_path):
        os.remove(db_path)
    
    conn = sqlite3.connect(db_path)
    conn.executescript(TARGET_SCHEMA)
    
    cols = ["source", "source_url", "title", "artist", "title_slug", "artist_slug",
            "version_name", "rating", "votes", "key_base", "difficulty", "category",
            "region", "chords", "chord_count", "line_count", "content",
            "content_hash", "scraped_at"]
    placeholders = ",".join("?" for _ in cols)
    conn.executemany(
        f"INSERT INTO songs ({','.join(cols)}) VALUES ({placeholders})",
        [tuple(r[c] for c in cols) for r in rows]
    )
    conn.commit()
    conn.close()

    print(f"\nFresh DB created with {len(rows)} songs at {db_path} ({os.path.getsize(db_path)} bytes)")

    # Compress to catalog.db.xz
    xz_path = r"C:\KordApp-AGY\app\src\main\assets\catalog.db.xz"
    with open(db_path, "rb") as f_in:
        data = f_in.read()
    with lzma.open(xz_path, "wb", preset=6) as f_out:
        f_out.write(data)
    print(f"Compressed fresh catalog to {xz_path} ({os.path.getsize(xz_path)} bytes)")

if __name__ == "__main__":
    main()
