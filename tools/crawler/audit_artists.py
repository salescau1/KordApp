import csv
import sys
from collections import Counter

sys.stdout.reconfigure(encoding='utf-8')

# 1. Read curated_artists.csv
curated = {}
curated_canonical = set()
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    r = csv.reader(f)
    header = next(r)
    for row in r:
        if row:
            name, reg, priority = row[0].strip(), row[1].strip(), row[2].strip() if len(row)>2 else '1'
            curated[name.lower()] = (name, reg, priority)
            curated_canonical.add(name.lower().replace(" ", "").replace("-", "").replace(".", ""))

print(f"Total current curated: {len(curated)}")
print("Region counts:", Counter(v[1] for v in curated.values()))

def is_curated(name):
    norm = name.strip().lower()
    can = norm.replace(" ", "").replace("-", "").replace(".", "")
    return (norm in curated) or (can in curated_canonical)

# 2. Check _artist_enriched.csv
enriched_missing = []
with open(r'C:\ChordApp\ug_crawler\data\ug\_artist_enriched.csv', 'r', encoding='utf-8') as f:
    r = csv.DictReader(f)
    for row in r:
        buang = row.get('\ufeffbuang', '') or row.get('buang', '')
        if buang == '1':
            continue
        art = row['artist'].strip()
        if not is_curated(art):
            try:
                lagu = int(row.get('lagu', 0) or 0)
            except:
                lagu = 0
            enriched_missing.append({
                'artist': art,
                'region': row.get('region', ''),
                'country': row.get('country', ''),
                'genre': row.get('genre', ''),
                'era': row.get('era', ''),
                'lagu': lagu
            })

# Sort by song count descending
enriched_missing.sort(key=lambda x: x['lagu'], reverse=True)

print(f"\nTotal non-discarded artists in _artist_enriched missing from curated: {len(enriched_missing)}")

indo_missing = [x for x in enriched_missing if x['region'] == 'INDO' or x['country'] in ('ID', 'Indonesia')]
barat_missing = [x for x in enriched_missing if x['region'] == 'BARAT' or x['country'] in ('US', 'GB', 'UK', 'CA', 'AU')]
asia_missing = [x for x in enriched_missing if x['region'] in ('JAPAN', 'KOREA', 'ASIA')]

print(f"\n--- TOP MISSING INDO ARTISTS ({len(indo_missing)} total) ---")
for x in indo_missing[:40]:
    print(f"  {x['artist']:<30} | {x['lagu']:>3} songs | {x['genre']} | {x['era']}")

print(f"\n--- TOP MISSING BARAT ARTISTS ({len(barat_missing)} total) ---")
for x in barat_missing[:50]:
    print(f"  {x['artist']:<30} | {x['lagu']:>3} songs | {x['genre']} | {x['era']}")
