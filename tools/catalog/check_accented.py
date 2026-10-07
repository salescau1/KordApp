import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

conn = sqlite3.connect("tools/catalog/db_batches/batch_accented.db")
c = conn.cursor()
c.execute("SELECT COUNT(*), COUNT(DISTINCT artist) FROM songs")
cnt, art_cnt = c.fetchone()
print(f"batch_accented.db: {cnt} songs from {art_cnt} artists")

c.execute("SELECT artist, COUNT(*) FROM songs GROUP BY artist")
for r in c.fetchall():
    print(f"  {r[0]}: {r[1]} songs")
