import sqlite3
import glob
import csv
import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

# 1. Full merged DB
conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()
rows = c.execute("SELECT id, source, artist, title, version_name, source_url FROM songs WHERE artist_slug LIKE '%plain-white%' OR artist LIKE '%Plain White%'").fetchall()
print(f"Total songs in full_merged_catalog.db for Plain White T's: {len(rows)}")
sources = {}
for r in rows:
    src = r[1]
    sources[src] = sources.get(src, 0) + 1
print(f"Sources distribution: {sources}")
for r in rows[:10]:
    print("  ", r)

# 2. Check batch CSVs
for f in sorted(glob.glob("tools/catalog/batches/batch_*.csv")):
    with open(f, encoding="utf-8") as fp:
        for row in csv.DictReader(fp):
            if "plain white" in row["artist"].lower():
                print(f"Found in {f}: {row}")

# 3. Check GT index
tsv = Path("C:/ChordApp/guitartuna_crawler/data/gt/artist_index.tsv")
if tsv.exists():
    for line in tsv.read_text(encoding="utf-8").splitlines():
        if "plain white" in line.lower():
            print("GT index match:", line)

from crawler.batch_crawler import discover_gt_songs, load_gt_artist_index

index = load_gt_artist_index()
url = index.get("plain white t's")
print(f"URL for Plain White T's in GT index: {url}")

songs = discover_gt_songs("Plain White T's", url)
print(f"Discovered GT songs: {len(songs)}")
for s in songs[:10]:
    print("  ", s)

