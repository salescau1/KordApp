import datetime
import hashlib
import html
import json
import re
import sqlite3
import sys
import unicodedata
import urllib.request
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.normalizer import normalize_sheet, extract_unique_chords

headers = {'User-Agent': 'Mozilla/5.0'}

def to_slug(text: str) -> str:
    s = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode('utf-8')
    s = re.sub(r'[^a-zA-Z0-9]+', '-', s).strip('-').lower()
    return s

def fetch_html(url: str) -> str:
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=12) as resp:
            return resp.read().decode('utf-8', errors='replace')
    except Exception as e:
        print(f"  [FETCH ERR] {url}: {e}", flush=True)
        return ""

def parse_and_insert(conn, url, artist, title, region="BARAT"):
    html_text = fetch_html(url)
    m = re.search(r'class="js-store"\s+data-content="([^"]+)"', html_text)
    if not m:
        print(f"No js-store for {title}")
        return
    try:
        data = json.loads(html.unescape(m.group(1)))
    except Exception:
        return

    page_data = data.get("store", {}).get("page", {}).get("data", {})
    raw_content = page_data.get("tab_view", {}).get("wiki_tab", {}).get("content", "")
    if not raw_content:
        return

    tab = page_data.get("tab", {})
    votes = tab.get("votes") or 0
    rating = tab.get("rating") or 5.0
    key_base = tab.get("tonality_name") or ""
    difficulty = tab.get("difficulty") or "intermediate"

    cleaned = re.sub(r'(?i)\[/?tab\]', '', raw_content)
    cleaned = re.sub(r'(?i)\[ch\](.*?)\[/ch\]', r'[\1]', cleaned)
    sheet = normalize_sheet(cleaned)
    if not sheet:
        return
    unique_chords = extract_unique_chords(sheet)
    if not unique_chords:
        return

    c = conn.cursor()
    c.execute("""
        INSERT OR REPLACE INTO songs (
            source, source_url, title, artist, title_slug, artist_slug,
            version_name, rating, votes, key_base, difficulty, category,
            region, chords, chord_count, line_count, content, content_hash, scraped_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        "UG", url, title, artist, to_slug(title), to_slug(artist),
        "Ver 1", float(rating), int(votes), key_base, difficulty,
        "Soundtrack", region, ",".join(unique_chords), len(unique_chords),
        len(sheet.splitlines()), sheet, hashlib.md5(sheet.encode()).hexdigest(),
        datetime.datetime.utcnow().isoformat()
    ))
    conn.commit()
    print(f"  + [SAVED] {artist} - {title} ({to_slug(artist)}/{to_slug(title)})", flush=True)

MORE_TABS = [
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/top-gun-danger-zone-chords-1777942", "Kenny Loggins", "Danger Zone", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/a-star-is-born-always-remember-us-this-way-chords-2484211", "Lady Gaga", "Always Remember Us This Way", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/frozen-2-into-the-unknown-chords-2904659", "Idina Menzel", "Into the Unknown", "BARAT"),
]

def main():
    db_path = str(TOOLS_DIR / "catalog" / "db_batches" / "batch_ost.db")
    conn = sqlite3.connect(db_path)
    for url, art, tit, reg in MORE_TABS:
        parse_and_insert(conn, url, art, tit, reg)
    conn.close()

if __name__ == '__main__':
    main()
