import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

conn = sqlite3.connect('Catalog/chordtela.db')
c = conn.cursor()

print("ChordTela sanity check:")
print("Total rows:", c.execute("SELECT count(*) FROM songs").fetchone()[0])
print("Distinct URLs:", c.execute("SELECT count(DISTINCT source_url) FROM songs").fetchone()[0])
print("Null titles:", c.execute("SELECT count(*) FROM songs WHERE title IS NULL OR title = ''").fetchone()[0])
print("Null artists:", c.execute("SELECT count(*) FROM songs WHERE artist IS NULL OR artist = ''").fetchone()[0])
print("Null contents:", c.execute("SELECT count(*) FROM songs WHERE content IS NULL OR content = ''").fetchone()[0])
print("Distinct regions in CT:", c.execute("SELECT DISTINCT region FROM songs").fetchall())
print("Null slugs:", c.execute("SELECT count(*) FROM songs WHERE title_slug IS NULL OR artist_slug IS NULL").fetchone()[0])

# Check artist casing in chordtela
upper_artists = c.execute("SELECT count(*) FROM songs WHERE artist = UPPER(artist) AND length(artist) > 3").fetchone()[0]
print("Upper-case artists in CT:", upper_artists)

conn.close()
