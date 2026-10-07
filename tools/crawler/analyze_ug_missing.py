import sqlite3
import csv
import sys

sys.stdout.reconfigure(encoding='utf-8')

# Read current curated
curated = {}
curated_clean = set()
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    r = csv.reader(f)
    next(r)
    for row in r:
        if row:
            name, reg, priority = row[0].strip(), row[1].strip(), row[2].strip() if len(row)>2 else '1'
            curated[name.lower()] = (name, reg, priority)
            clean = "".join(ch for ch in name.lower() if ch.isalnum())
            curated_clean.add(clean)

def is_curated(name):
    nl = name.strip().lower()
    if nl in curated:
        return True
    return "".join(ch for ch in nl if ch.isalnum()) in curated_clean

# Check UG catalog db
con_ug = sqlite3.connect(r'C:\ChordApp\ug_crawler\data\ug\ug_catalog.db')
ug_artists = con_ug.execute("""
    SELECT artist, count(*) as cnt 
    FROM songs 
    WHERE artist IS NOT NULL AND artist != ''
    GROUP BY artist
    ORDER BY cnt DESC
""").fetchall()
con_ug.close()

missing_ug = [(art, cnt) for art, cnt in ug_artists if not is_curated(art)]
print(f"Total artists in ug_catalog.db: {len(ug_artists)}")
print(f"Artists in ug_catalog.db already in curated: {len(ug_artists) - len(missing_ug)}")
print(f"Artists in ug_catalog.db NOT in curated: {len(missing_ug)}")

# Print Top 60 missing artists in ug_catalog.db
print("\nTop 60 artists in ug_catalog.db missing from curated:")
for art, cnt in missing_ug[:60]:
    print(f"  {art:<35} | {cnt:>3} songs")
