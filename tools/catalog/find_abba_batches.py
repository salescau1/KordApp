import glob
import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

for f in sorted(glob.glob("tools/catalog/db_batches/batch_*.db")):
    c = sqlite3.connect(f)
    r = c.execute("SELECT id, source, title, artist, source_url FROM songs WHERE source_url LIKE '%685c1d20%'").fetchone()
    if r:
        print(f"Found in {f}: {r}")

