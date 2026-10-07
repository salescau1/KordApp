import glob
import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

db_files = sorted(glob.glob('tools/catalog/db_batches/batch_*.db'))
for db_path in db_files:
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    for r in c.execute("SELECT artist, title, artist_slug, title_slug FROM songs WHERE artist LIKE '%Davichi%' OR artist LIKE '%Gummy%'"):
        print(f"{r[0]} - {r[1]} ({r[2]}/{r[3]})")
    conn.close()
