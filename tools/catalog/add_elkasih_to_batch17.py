import html
import json
import re
import sqlite3
import sys
import time
import unicodedata
import urllib.request
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.parsers.ultimate_guitar import parse_ug_html
from crawler.normalizer import extract_unique_chords

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

ELKASIH_TABS = [
    ("Kau Tigakan Cinta", "https://tabs.ultimate-guitar.com/tab/elkasih/kau-tigakan-cintaku-chords-2740623"),
    ("Kau Tigakan Cinta", "https://tabs.ultimate-guitar.com/tab/elkasih/kau-3kan-cintaku-chords-1815797"),
    ("Lelakimu", "https://tabs.ultimate-guitar.com/tab/elkasih/lelakimu-chords-2740632"),
    ("Pesan Dari Surga", "https://tabs.ultimate-guitar.com/tab/elkasih/pesan-dari-surga-chords-2740635"),
    ("Pergilah Cinta", "https://tabs.ultimate-guitar.com/tab/elkasih/pergilah-cinta-chords-2740629"),
    ("Aku Harus Pergi", "https://tabs.ultimate-guitar.com/tab/elkasih/aku-harus-pergi-chords-2740620"),
    ("Dimana Kau Punya Hati", "https://tabs.ultimate-guitar.com/tab/elkasih/dimana-kau-punya-hati-chords-2731362"),
]

def to_slug(text: str) -> str:
    s = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode('utf-8')
    return re.sub(r'[^a-zA-Z0-9]+', '-', s).strip('-').lower()

def main():
    db_path = str(TOOLS_DIR / "catalog" / "db_batches" / "batch_17.db")
    print(f"Adding Elkasih songs to {db_path}...")
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    added = 0
    for title_pref, url in ELKASIH_TABS:
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=12) as resp:
                html_text = resp.read().decode('utf-8', errors='replace')
            parsed = parse_ug_html(html_text, url, artist_hint="Elkasih", title_hint=title_pref)
            if not parsed or not parsed.content:
                print(f"  ! Failed parsing: {url}")
                continue

            artist = "Elkasih"
            title = title_pref
            artist_slug = to_slug(artist)
            title_slug = to_slug(title)

            chords_str = ",".join(parsed.chords) if isinstance(parsed.chords, list) else (parsed.chords or "")
            c.execute("""
                INSERT OR REPLACE INTO songs (
                    source, source_url, title, artist, title_slug, artist_slug,
                    version_name, rating, votes, key_base, difficulty, category,
                    region, chords, chord_count, line_count, content, content_hash, scraped_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                "UG", url, title, artist, title_slug, artist_slug,
                parsed.version_name, parsed.rating, parsed.votes, parsed.key_base, parsed.difficulty, "Pop",
                "INDO", chords_str, parsed.chord_count, parsed.line_count, parsed.content, parsed.content_hash,
                "2026-10-06T19:45:00"
            ))
            conn.commit()
            added += 1
            print(f"  + Added: {artist} - {title} ({parsed.version_name})")
            time.sleep(0.3)
        except Exception as e:
            print(f"  ! Error on {url}: {e}")

    conn.close()
    print(f"\nDone! Added {added} Elkasih songs to batch_17.db.")

if __name__ == "__main__":
    main()
