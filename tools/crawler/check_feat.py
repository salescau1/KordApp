import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8')

for b in [1, 2, 3, 4]:
    db = rf'C:\KordApp-AGY\tools\catalog\db_batches\batch_{b:02d}.db'
    con = sqlite3.connect(db)
    rows = con.execute("SELECT artist, title FROM songs WHERE artist LIKE '%feat%' OR artist LIKE '%ft.%' OR artist LIKE '%,%' LIMIT 10").fetchall()
    print(f"Batch {b:02d} feat samples:")
    for r in rows:
        print("  Artist:", r[0], "--> Title:", r[1])
    con.close()
