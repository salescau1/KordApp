import csv
import sqlite3
import re
import unicodedata
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8')

def slugify(text: str) -> str:
    text = unicodedata.normalize('NFD', text).encode('ascii', 'ignore').decode('utf-8')
    text = re.sub(r'[^\w\s-]', '', text.lower()).strip()
    return re.sub(r'[-\s]+', '-', text)

# 1. Load curated artists (813)
curated = []
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    for r in csv.reader(f):
        if r:
            curated.append({'name': r[0].strip(), 'region': r[1].strip()})
artists = curated[1:]

# 2. Load reference sources
enriched_ref = {}
with open(r'C:\ChordApp\ug_crawler\data\ug\_artist_enriched.csv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        nm = row['artist'].strip().lower()
        enriched_ref[nm] = row
        clean = re.sub(r'[^\w]', '', nm)
        enriched_ref[clean] = row

union_ref = {}
with open(r'C:\ChordApp\ug_crawler\data\ug\artist_union.csv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        nm = row['artist'].strip().lower()
        union_ref[nm] = row
        clean = re.sub(r'[^\w]', '', nm)
        union_ref[clean] = row

ug_songs = {}
try:
    con = sqlite3.connect(r'C:\ChordApp\ug_crawler\data\ug\ug_catalog.db')
    for r in con.execute("SELECT lower(trim(artist)), count(*) FROM songs GROUP BY lower(trim(artist))"):
        ug_songs[r[0]] = r[1]
        ug_songs[re.sub(r'[^\w]', '', r[0])] = r[1]
    con.close()
except:
    pass

gt_songs = {}
try:
    con = sqlite3.connect(r'C:\ChordApp\guitartuna_crawler\guitartuna_merged.db')
    for r in con.execute("SELECT lower(trim(artist)), count(*) FROM songs GROUP BY lower(trim(artist))"):
        gt_songs[r[0]] = r[1]
        gt_songs[re.sub(r'[^\w]', '', r[0])] = r[1]
    con.close()
except:
    pass

enriched_list = []
for a in artists:
    name = a['name']
    region = a['region']
    norm = name.lower()
    clean = re.sub(r'[^\w]', '', norm)

    ref = enriched_ref.get(norm) or enriched_ref.get(clean) or union_ref.get(norm) or union_ref.get(clean) or {}
    
    country = ref.get('country', '')
    if not country:
        if region == 'INDO':
            country = 'Indonesia'
        elif region == 'JAPAN':
            country = 'Japan'
        elif region == 'KOREA':
            country = 'South Korea'
        else:
            country = 'United States'
    
    genre = ref.get('genre', '')
    if not genre:
        if region == 'INDO':
            genre = 'pop|pop rock|indie'
        elif region == 'JAPAN':
            genre = 'j-pop|rock|anime'
        elif region == 'KOREA':
            genre = 'k-pop|r&b|ballad'
        else:
            genre = 'rock|pop'
            
    era = ref.get('era', '')
    if not era:
        era = '2000s'

    try:
        lagu = int(ref.get('lagu', 0) or 0)
    except:
        lagu = 0

    if lagu == 0:
        lagu = ug_songs.get(norm) or ug_songs.get(clean) or gt_songs.get(norm) or gt_songs.get(clean) or 25

    slug = ref.get('artist_slug', '') or slugify(name)

    enriched_list.append({
        'artist': name,
        'country': country,
        'genre': genre,
        'era': era,
        'region': region,
        'lagu': int(lagu),
        'artist_slug': slug
    })

# 3. Save master enriched CSV
out_enriched_path = r'C:\KordApp-AGY\tools\catalog\curated_artists_enriched.csv'
with open(out_enriched_path, 'w', encoding='utf-8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=['artist', 'region', 'country', 'genre', 'era', 'lagu', 'artist_slug'])
    writer.writeheader()
    writer.writerows(enriched_list)

print(f"Master enriched CSV written to: {out_enriched_path}")

# 4. Split into 15 balanced batches (LPT / greedy bin-packing by lagu)
NUM_BATCHES = 15
buckets = [[] for _ in range(NUM_BATCHES)]
bucket_weights = [0] * NUM_BATCHES

# Sort artists by lagu descending
sorted_artists = sorted(enriched_list, key=lambda x: x['lagu'], reverse=True)

for art in sorted_artists:
    # Choose bucket with minimum total weight
    min_idx = bucket_weights.index(min(bucket_weights))
    buckets[min_idx].append(art)
    bucket_weights[min_idx] += art['lagu']

# Sort each bucket alphabetically by artist name
for b in buckets:
    b.sort(key=lambda x: x['artist'].lower())

# Save 15 batch files
batch_dir = Path(r'C:\KordApp-AGY\tools\catalog\batches')
batch_dir.mkdir(parents=True, exist_ok=True)

print(f"\n--- BATCH DISTRIBUTION (15 BATCHES) ---")
for idx, (b, weight) in enumerate(zip(buckets, bucket_weights), 1):
    batch_file = batch_dir / f"batch_{idx:02d}.csv"
    with open(batch_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['artist', 'region', 'country', 'genre', 'era', 'lagu', 'artist_slug'])
        writer.writeheader()
        writer.writerows(b)
    
    indo_c = sum(1 for x in b if x['region'] == 'INDO')
    barat_c = sum(1 for x in b if x['region'] == 'BARAT')
    asia_c = sum(1 for x in b if x['region'] in ('JAPAN', 'KOREA'))
    sample_art = ", ".join(x['artist'] for x in b[:3])
    print(f"Batch {idx:02d}: {len(b):>2} artis | Est. Songs: {weight:>4} | (INDO: {indo_c:>2}, BARAT: {barat_c:>2}, ASIA: {asia_c:>2}) | Sample: {sample_art}...")
