import sqlite3
import csv
import sys
from collections import Counter

sys.stdout.reconfigure(encoding='utf-8')

CURATED_PATH = r'C:\KordApp-AGY\tools\catalog\curated_artists.csv'

curated = set()
curated_clean = set()
with open(CURATED_PATH, 'r', encoding='utf-8') as f:
    for r in csv.reader(f):
        if r:
            name = r[0].strip().lower()
            curated.add(name)
            curated_clean.add("".join(c for c in name if c.isalnum()))

def is_curated(name):
    norm = name.strip().lower()
    if norm in curated:
        return True
    return "".join(c for c in norm if c.isalnum()) in curated_clean

# 1. Inspect _artist_enriched.csv for candidates with >= 40 songs
enriched_candidates = []
EXCLUDED_KEYWORDS = ['misc', 'soundtrack', 'traditional', 'hymn', 'various', 'theme', 'christmas', 'nursery', 'disney', 'children', 'tunes', 'unknown', 'karaoke', 'tribute']

with open(r'C:\ChordApp\ug_crawler\data\ug\_artist_enriched.csv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        art = row['artist'].strip()
        if not art:
            continue
        art_lower = art.lower()
        if any(ex in art_lower for ex in EXCLUDED_KEYWORDS):
            continue
        if is_curated(art):
            continue
        try:
            lagu = int(row.get('lagu', 0) or 0)
        except:
            lagu = 0
        if lagu >= 30:
            enriched_candidates.append({
                'artist': art,
                'region': row.get('region', 'en'),
                'country': row.get('country', ''),
                'genre': row.get('genre', ''),
                'era': row.get('era', ''),
                'lagu': lagu
            })

enriched_candidates.sort(key=lambda x: x['lagu'], reverse=True)
print(f"Total high-song candidates from _artist_enriched (>=30 songs): {len(enriched_candidates)}")

# 2. Check UG catalog db directly
ug_db_candidates = []
con_ug = sqlite3.connect(r'C:\ChordApp\ug_crawler\data\ug\ug_catalog.db')
for row in con_ug.execute("SELECT artist, count(*) as cnt FROM songs GROUP BY artist HAVING cnt >= 25 ORDER BY cnt DESC"):
    art = row[0].strip()
    cnt = row[1]
    art_lower = art.lower()
    if any(ex in art_lower for ex in EXCLUDED_KEYWORDS):
        continue
    if not is_curated(art):
        ug_db_candidates.append((art, cnt))
con_ug.close()
print(f"Total candidates from ug_catalog.db (>=25 songs): {len(ug_db_candidates)}")

# Let's inspect the top 50 from enriched
print("\n--- TOP 50 ENRICHED CANDIDATES ---")
for c in enriched_candidates[:50]:
    print(f"  {c['artist']:<35} | {c['lagu']:>3} songs | {c['genre']} | {c['country']}")

print("\n--- TOP 50 UG DB CANDIDATES ---")
for art, cnt in ug_db_candidates[:50]:
    print(f"  {art:<35} | {cnt:>3} songs")
