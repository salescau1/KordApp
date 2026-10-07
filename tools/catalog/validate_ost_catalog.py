import re
import sqlite3
import glob
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent.parent
KT_PATH = TOOLS_DIR.parent / "app" / "src" / "main" / "java" / "com" / "chordapp" / "ui" / "screens" / "explore" / "OstCatalogData.kt"

with open(KT_PATH, encoding='utf-8') as f:
    text = f.read()

pattern = r'OstSongItem\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"'
items = re.findall(pattern, text)
print(f"Total OstSongItem entries found in OstCatalogData.kt: {len(items)}")

all_dbs = [str(TOOLS_DIR / "catalog" / "full_merged_catalog.db")]

def find_in_dbs(a_slug, t_slug):
    for db in all_dbs:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        c.execute("SELECT artist, title FROM songs WHERE artist_slug = ? AND title_slug = ?", (a_slug, t_slug))
        r = c.fetchone()
        conn.close()
        if r:
            return r
    return None

def search_similar(a_slug, t_slug):
    for db in all_dbs:
        conn = sqlite3.connect(db)
        c = conn.cursor()
        c.execute("SELECT artist, title, artist_slug, title_slug FROM songs WHERE title_slug LIKE ? LIMIT 1", (f"%{t_slug[:6]}%",))
        r = c.fetchone()
        conn.close()
        if r:
            return r
    return None

missing = []
for title, artist, a_slug, t_slug in items:
    found = find_in_dbs(a_slug, t_slug)
    if not found:
        sim = search_similar(a_slug, t_slug)
        missing.append((title, artist, a_slug, t_slug, sim))

if missing:
    print(f"MISSING ({len(missing)}):")
    for m in missing:
        print(f"  - '{m[0]}' by '{m[1]}' ({m[2]} / {m[3]}) => Suggested: {m[4]}")
else:
    print("SUCCESS: 100% of all OstSongItem entries match the databases!")
