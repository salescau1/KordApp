import sqlite3
import csv
import re
from collections import Counter

con = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = con.cursor()

# 1. OST with proper regex or word boundaries
c.execute("SELECT artist, title, source_url FROM songs")
ost_songs = []
for artist, title, url in c.fetchall():
    # Check if OST or Soundtrack is a distinct token
    if re.search(r'\b(ost|soundtrack)\b', title, re.IGNORECASE) or re.search(r'\bost\b', url, re.IGNORECASE):
        ost_songs.append((artist, title, url))

print(f"Proper OST songs count: {len(ost_songs)}")
print("Sample proper OST songs:")
for a, t, u in ost_songs[:10]:
    print(f"  {a} - {t}")

# 2. Check _artist_enriched.csv from C:\ChordApp\ug_crawler\data\ug\_artist_enriched.csv
enriched_full = {}
with open(r'C:\ChordApp\ug_crawler\data\ug\_artist_enriched.csv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        art = row['artist'].strip()
        enriched_full[art.lower()] = row
        clean = re.sub(r'[^\w]', '', art.lower())
        enriched_full[clean] = row

c.execute("SELECT DISTINCT artist FROM songs")
db_artists = [r[0] for r in c.fetchall()]
matched = 0
rock90_artists = []
dangdut_artists = []
pop_artists = []
rock_artists = []

for a in db_artists:
    low = a.strip().lower()
    clean = re.sub(r'[^\w]', '', low)
    ref = enriched_full.get(low) or enriched_full.get(clean)
    if ref:
        matched += 1
        g = ref.get('genre', '').lower()
        era = ref.get('era', '').lower()
        if 'rock' in g and ('1990' in era or '90' in era):
            rock90_artists.append(a)
        if 'dangdut' in g:
            dangdut_artists.append(a)
        if 'rock' in g:
            rock_artists.append(a)
        if 'pop' in g:
            pop_artists.append(a)

print(f"\nTotal matched in _artist_enriched: {matched} / {len(db_artists)}")
print(f"Rock 90s artists ({len(rock90_artists)}):", rock90_artists[:15])
print(f"Dangdut artists ({len(dangdut_artists)}):", dangdut_artists[:15])
print(f"Rock artists count: {len(rock_artists)}")
print(f"Pop artists count: {len(pop_artists)}")
