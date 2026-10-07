import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')
conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()
for q in ['Lovers', 'Toumei Datta Sekai', 'New Genesis', 'Worry', 'Remember Me']:
    print(f"=== {q} ===")
    for r in c.execute("SELECT artist, title, artist_slug, title_slug FROM songs WHERE title LIKE ?", (f"%{q}%",)).fetchall():
        if any(x in r[0].lower() for x in ['7', 'hata', 'ado', 'lee', 'gummy', 'naruto']):
            print(f"  {r[0]} - {r[1]} -> (\"{r[2]}\", \"{r[3]}\")")
conn.close()
