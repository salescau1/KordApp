import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')
conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()
print('Total songs in full_merged_catalog:', c.execute('SELECT count(*) FROM songs').fetchone()[0])
print('Michael Buble count:', c.execute("SELECT count(*) FROM songs WHERE artist LIKE '%Bubl%' OR artist_slug LIKE '%buble%'").fetchone()[0])
print('Beyonce count:', c.execute("SELECT count(*) FROM songs WHERE artist LIKE '%Beyonc%' OR artist_slug LIKE '%beyonce%'").fetchone()[0])
print('Billy Crudup count:', c.execute("SELECT count(*) FROM songs WHERE artist_slug = 'billy-crudup'").fetchone()[0])
print('Billy Crudup songs:', [r[0] for r in c.execute("SELECT title FROM songs WHERE artist_slug = 'billy-crudup'").fetchall()])
conn.close()
