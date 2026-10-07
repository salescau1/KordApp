import sqlite3
import csv
import glob
import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))
from crawler.batch_crawler import load_gt_artist_index

gt_index = load_gt_artist_index()

dbs_dir = TOOLS_DIR / "catalog" / "db_batches"
batches_dir = TOOLS_DIR / "catalog" / "batches"

all_missed = []

for i in range(1, 16):
    db_f = dbs_dir / f"batch_{i:02d}.db"
    csv_f = batches_dir / f"batch_{i:02d}.csv"
    if not db_f.exists() or not csv_f.exists():
        continue

    conn = sqlite3.connect(str(db_f))
    gt_artists_in_db = set(r[0].lower().strip() for r in conn.execute("SELECT DISTINCT artist FROM songs WHERE source = 'GT'").fetchall())

    with open(csv_f, "r", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            art = row["artist"]
            reg = row.get("region", "BARAT")
            if art.lower().strip() not in gt_artists_in_db:
                # check if in gt_index
                gt_url = gt_index.get(art.lower().strip())
                if gt_url:
                    all_missed.append((i, art, reg, gt_url))
    conn.close()

print(f"Total artists across all 15 batches in GT index with 0 GT songs: {len(all_missed)}")
for b_num, art, reg, url in all_missed:
    print(f"  Batch {b_num:02d}: {art:25s} [{reg}] -> {url}")
