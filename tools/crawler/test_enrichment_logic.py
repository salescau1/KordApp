import csv
import sqlite3
import re
import unicodedata

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
header = curated[0]
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

# Song count maps
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

ct_songs = {}
try:
    con = sqlite3.connect(r'C:\KordApp-AGY\Catalog\chordtela.db')
    for r in con.execute("SELECT lower(trim(artist)), count(*) FROM songs GROUP BY lower(trim(artist))"):
        ct_songs[r[0]] = r[1]
        ct_songs[re.sub(r'[^\w]', '', r[0])] = r[1]
    con.close()
except:
    pass

enriched_list = []
missing_metadata = []

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
            country = 'United States' # default barat
    
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

    # Estimate lagu (songs)
    try:
        lagu = int(ref.get('lagu', 0) or 0)
    except:
        lagu = 0

    if lagu == 0:
        # Check ug_songs, gt_songs, ct_songs
        lagu = ug_songs.get(norm) or ug_songs.get(clean) or gt_songs.get(norm) or gt_songs.get(clean) or ct_songs.get(norm) or ct_songs.get(clean) or 25

    slug = ref.get('artist_slug', '') or slugify(name)

    enriched_list.append({
        'artist': name,
        'country': country,
        'genre': genre,
        'era': era,
        'region': region,
        'lagu': lagu,
        'artist_slug': slug
    })

print(f"Total enriched artists: {len(enriched_list)}")
print("Sample enriched:")
for item in enriched_list[:5]:
    print(" ", item)
print("\nSample Indo enriched:")
for item in [x for x in enriched_list if x['region']=='INDO'][:5]:
    print(" ", item)
