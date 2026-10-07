import sqlite3
import csv
import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))
from crawler.batch_crawler import load_gt_artist_index

gt_index = load_gt_artist_index()

conn = sqlite3.connect("tools/catalog/db_batches/batch_08.db")
gt_artists = set(r[0].lower().strip() for r in conn.execute("SELECT DISTINCT artist FROM songs WHERE source = 'GT'").fetchall())

with open("tools/catalog/batches/batch_08.csv", encoding="utf-8") as f:
    for row in csv.DictReader(f):
        art = row["artist"]
        if art.lower().strip() not in gt_artists:
            ug_count = conn.execute("SELECT count(*) FROM songs WHERE lower(artist) = ?", (art.lower(),)).fetchone()[0]
            in_gt = art.lower().strip() in gt_index
            gt_url = gt_index.get(art.lower().strip(), "")
            print(f"Missing GT: {art:25s} | in GT index: {str(in_gt):5s} | UG songs: {ug_count} | GT url: {gt_url}")
