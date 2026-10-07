import sqlite3
import re
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

# Check how many songs have explicit (ost), [ost], ost., soundtrack, etc.
c.execute("""
    SELECT artist, title, source_url
    FROM songs 
    WHERE lower(title) LIKE '%(ost%' 
       OR lower(title) LIKE '%[ost%'
       OR lower(title) LIKE '%ost.%'
       OR lower(title) LIKE '% ost'
       OR lower(title) LIKE '%soundtrack%'
       OR lower(source_url) LIKE '%-ost-%'
    GROUP BY artist_slug, title_slug
    ORDER BY artist ASC, title ASC
""")
rows = c.fetchall()
print(f"Total explicit OST songs in database: {len(rows)}")
print("\nSample explicit OST songs (first 20):")
for r in rows[:20]:
    print(r)

# Check specific artists
artists_to_check = ['akeboshi', 'radwimps', 'lisa', 'ikimonogakari', 'kana-boon', 'flow', 'sherina', 'melly-goeslaw', 'acha-septriasa', 'irwansyah', 'nidji', 'rossa']
print("\n--- Artist Presence Check ---")
for a in artists_to_check:
    c.execute("SELECT COUNT(*) FROM songs WHERE artist_slug LIKE ?", (f"%{a}%",))
    count = c.fetchone()[0]
    c.execute("SELECT DISTINCT artist, artist_slug FROM songs WHERE artist_slug LIKE ? LIMIT 3", (f"%{a}%",))
    names = c.fetchall()
    print(f"{a}: {count} versions, matches: {names}")
