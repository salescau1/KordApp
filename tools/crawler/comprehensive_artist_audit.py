import csv
import sqlite3
import sys
from collections import Counter

sys.stdout.reconfigure(encoding='utf-8')

# 1. Existing curated artists
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

def is_in_curated(name: str) -> bool:
    nl = name.strip().lower()
    if nl in curated:
        return True
    clean = "".join(ch for ch in nl if ch.isalnum())
    return clean in curated_clean

# 2. Extract artists from ChordTela DB (Top Indonesian artists)
ct_path = r'C:\KordApp-AGY\Catalog\chordtela.db'
ct_artists = Counter()
try:
    con_ct = sqlite3.connect(ct_path)
    for row in con_ct.execute("SELECT artist, count(*) FROM songs WHERE artist IS NOT NULL GROUP BY artist"):
        name = row[0].strip()
        cnt = row[1]
        if name and not is_in_curated(name):
            ct_artists[name] += cnt
    con_ct.close()
except Exception as e:
    print("CT error:", e)

# 3. Extract artists from UG catalog DB
ug_path = r'C:\ChordApp\ug_crawler\data\ug\ug_catalog.db'
ug_artists = Counter()
try:
    con_ug = sqlite3.connect(ug_path)
    for row in con_ug.execute("SELECT artist, count(*) FROM songs WHERE artist IS NOT NULL GROUP BY artist"):
        name = row[0].strip()
        cnt = row[1]
        if name and not is_in_curated(name):
            ug_artists[name] += cnt
    con_ug.close()
except Exception as e:
    print("UG error:", e)

# 4. Extract artists from GT merged DB
gt_path = r'C:\ChordApp\guitartuna_crawler\guitartuna_merged.db'
gt_artists = Counter()
try:
    con_gt = sqlite3.connect(gt_path)
    for row in con_gt.execute("SELECT artist, count(*) FROM songs WHERE artist IS NOT NULL GROUP BY artist"):
        name = row[0].strip()
        cnt = row[1]
        if name and not is_in_curated(name):
            gt_artists[name] += cnt
    con_gt.close()
except Exception as e:
    print("GT error:", e)

# 5. Extract from _artist_enriched.csv
enriched_missing = {}
try:
    with open(r'C:\ChordApp\ug_crawler\data\ug\_artist_enriched.csv', 'r', encoding='utf-8') as f:
        for row in csv.DictReader(f):
            art = row['artist'].strip()
            if art and not is_in_curated(art):
                try:
                    lagu = int(row.get('lagu', 0) or 0)
                except:
                    lagu = 0
                enriched_missing[art.lower()] = {
                    'name': art,
                    'region': row.get('region', ''),
                    'country': row.get('country', ''),
                    'genre': row.get('genre', ''),
                    'era': row.get('era', ''),
                    'lagu': lagu
                }
except Exception as e:
    print("Enriched error:", e)

print(f"Curated current count: {len(curated)}")
print(f"ChordTela missing artists: {len(ct_artists)}")
print(f"UG missing artists: {len(ug_artists)}")
print(f"GT missing artists: {len(gt_artists)}")

print("\n--- TOP 40 INDONESIAN ARTISTS IN CHORDTELA (NOT IN CURATED) ---")
for art, cnt in ct_artists.most_common(40):
    print(f"  {art:<35} | {cnt} songs in CT")

print("\n--- TOP 40 WESTERN ARTISTS IN UG (NOT IN CURATED) ---")
for art, cnt in ug_artists.most_common(40):
    print(f"  {art:<35} | {cnt} songs in UG")

print("\n--- TOP 40 ARTISTS IN GT (NOT IN CURATED) ---")
for art, cnt in gt_artists.most_common(40):
    print(f"  {art:<35} | {cnt} songs in GT")
