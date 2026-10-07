import sqlite3, csv, re
from collections import defaultdict

con = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = con.cursor()
c.execute('SELECT DISTINCT artist, artist_slug FROM songs')
db_artists = dict(c.fetchall())

enriched = {}
with open(r'tools\catalog\curated_artists_enriched.csv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        enriched[row['artist'].strip().lower()] = row
        clean = re.sub(r'[^\w]', '', row['artist'].lower())
        enriched[clean] = row

with open(r'C:\ChordApp\ug_crawler\data\ug\_artist_enriched.csv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        art = row['artist'].strip()
        if art.lower() not in enriched:
            enriched[art.lower()] = row
        clean = re.sub(r'[^\w]', '', art.lower())
        if clean not in enriched:
            enriched[clean] = row

cat_artists = defaultdict(set)

# Seeds
cat_artists['rock_90s'].update(['slank', 'dewa-19', 'boomerang', 'jamrud', 'sheila-on-7', 'gigi', 'rif', 'koil', 'pas-band', 'netral', 'bip', 'tipe-x', 'type-x', 'nirvana', 'oasis', 'green-day', 'radiohead', 'pearl-jam', 'soundgarden', 'foo-fighters', 'red-hot-chili-peppers', 'the-smashing-pumpkins', 'smashing-pumpkins', 'weezer', 'blink-182', 'no-doubt', 'blur', 'the-cranberries', 'cranberries', 'r-e-m', 'rem', 'silverchair', 'bush', 'collective-soul', 'stone-temple-pilots', 'the-offspring', 'offspring', 'garbage', 'counting-crows', 'third-eye-blind', 'matchbox-twenty', 'goo-goo-dolls', 'live'])
cat_artists['pop_indo'].update(['dewa-19', 'sheila-on-7', 'noah', 'peterpan', 'ungu', 'padi', 'ari-lasso', 'chrisye', 'glenn-fredly', 'tulus', 'raisa', 'judika', 'rizky-febian', 'afgan', 'kahitna', 'maliq-d-essentials', 'maliq-dessentials', 'fourtwnty', 'letto', 'kerispatih', 'ada-band', 'd-masiv', 'dmasiv', 'armada', 'wali', 'kangen-band', 'repvblik', 'geisha', 'kotak', 'vierratale', 'vierra', 'hivi', 'last-child', 'virgoun', 'anji', 'budi-doremi', 'yovie-nuno', 'yovie-and-nuno', 'samsons', 'naff', 'seventeen', 'drive'])
cat_artists['dangdut'].update(['rhoma-irama', 'denny-caknan', 'guyon-waton', 'happy-asmara', 'ndx-a-k-a', 'ndx-aka', 'gilga-sahid', 'didi-kempot', 'nella-kharisma', 'via-vallen', 'evie-tamala', 'mansyur-s', 'meggy-z', 'ayu-ting-ting', 'ikke-nurjanah', 'elvy-sukaesih', 'iis-dahlia', 'caca-handika', 'a-rafiq', 'imam-s-arifin', 'tasya-rosmala', 'farel-prayoga', 'difarina-indra', 'wandra', 'thomas-arya', 'inul-daratista'])
cat_artists['classic_rock'].update(['queen', 'the-beatles', 'beatles', 'led-zeppelin', 'pink-floyd', 'guns-n-roses', 'guns-n-roses', 'scorpions', 'bon-jovi', 'deep-purple', 'the-rolling-stones', 'rolling-stones', 'aerosmith', 'ac-dc', 'acdc', 'kiss', 'eagles', 'the-who', 'the-doors', 'doors', 'black-sabbath', 'iron-maiden', 'metallica', 'eric-clapton', 'dire-straits', 'creedence-clearwater-revival', 'boston', 'kansas', 'toto', 'journey', 'van-halen', 'def-leppard', 'whitesnake'])
cat_artists['indie_folk'].update(['fourtwnty', 'payung-teduh', 'danilla', 'fiersa-besari', 'banda-neira', 'nadin-amizah', 'efek-rumah-kaca', 'barasuara', 'the-adams', 'white-shoes-the-couples-company', 'mocca', 'float', 'endah-n-rhesa', 'pamungkas', 'hindia', 'feast', 'silampukau', 'dialog-dini-hari', 'stars-and-rabbit', 'sore', 'polyester-embassy', 'folk', 'nosstress', 'jason-ranti'])
cat_artists['anime_jrock'].update(['radwimps', 'one-ok-rock', 'lisa', 'asian-kung-fu-generation', 'yoasobi', 'kenshi-yonezu', 'eve', 'aimer', 'l-arc-en-ciel', 'flow', 'spyair', 'ikimono-gakari', 'kana-boon', 'burnout-syndromes', 'man-with-a-mission', 'tk-from-ling-tosite-sigure', 'ling-tosite-sigure', 'unison-square-garden', 'babymetal', 'scandal', 'official-hige-dandism', 'king-gnu', 'vaundy', 'mrs-green-apple'])

for art, slug in db_artists.items():
    low = art.lower().strip()
    clean = re.sub(r'[^\w]', '', low)
    ref = enriched.get(low) or enriched.get(clean)
    if not ref:
        continue
    g = ref.get('genre', '').lower()
    era = ref.get('era', '').lower()
    reg = ref.get('region', '').lower()
    cntry = ref.get('country', '').lower()

    if ('rock' in g or 'grunge' in g or 'metal' in g or 'punk' in g) and ('1990' in era or '90' in era):
        cat_artists['rock_90s'].add(slug)
    if 'dangdut' in g:
        cat_artists['dangdut'].add(slug)
    if ('classic rock' in g or 'hard rock' in g or 'rock' in g) and ('1970' in era or '1980' in era or '1960' in era):
        cat_artists['classic_rock'].add(slug)
    if 'indie' in g or 'folk' in g or 'acoustic' in g:
        cat_artists['indie_folk'].add(slug)
    if reg in ('japan', 'jp') or cntry == 'japan' or 'j-pop' in g or 'anime' in g or 'j-rock' in g:
        cat_artists['anime_jrock'].add(slug)
    if reg in ('id', 'indo') or cntry == 'indonesia':
        if 'pop' in g:
            cat_artists['pop_indo'].add(slug)

out_file = 'app/src/main/java/com/chordapp/ui/screens/explore/ExploreGenreData.kt'
with open(out_file, 'w', encoding='utf-8') as f:
    f.write('package com.chordapp.ui.screens.explore\n\n')
    f.write('data class ExploreGenre(\n')
    f.write('    val id: String,\n')
    f.write('    val title: String,\n')
    f.write('    val subtitle: String,\n')
    f.write('    val isSongs: Boolean = false,\n')
    f.write('    val artistSlugs: Set<String> = emptySet()\n')
    f.write(')\n\n')
    f.write('object ExploreGenres {\n')
    f.write('    val ALL: List<ExploreGenre> = listOf(\n')
    
    genres_def = [
        ('rock_90s', 'Rock 90s', 'Nirvana, Slank, Dewa 19, Jamrud...'),
        ('ost', 'OST & Soundtrack', 'Soundtrack Film, Sinetron & Anime', True),
        ('pop_indo', 'Pop Indo', 'Sheila On 7, Peterpan, Ungu, Padi...'),
        ('dangdut', 'Dangdut & Koplo', 'Denny Caknan, Rhoma Irama, Guyon Waton...'),
        ('classic_rock', 'Classic Rock', 'Queen, The Beatles, Led Zeppelin...'),
        ('indie_folk', 'Indie & Akustik', 'Fourtwnty, Payung Teduh, Hindia, Danilla...'),
        ('anime_jrock', 'Anime & J-Rock', 'RADWIMPS, ONE OK ROCK, LiSA, Yoasobi...')
    ]
    
    for gid, title, sub, *rest in genres_def:
        is_song = rest[0] if rest else False
        f.write('        ExploreGenre(\n')
        f.write(f'            id = "{gid}",\n')
        f.write(f'            title = "{title}",\n')
        f.write(f'            subtitle = "{sub}",\n')
        if is_song:
            f.write('            isSongs = true\n')
        else:
            slugs = sorted(list(cat_artists[gid]))
            slugs_str = ', '.join(f'"{s}"' for s in slugs)
            f.write(f'            artistSlugs = setOf({slugs_str})\n')
        f.write('        ),\n')
    f.write('    )\n')
    f.write('}\n')

print(f'Wrote {out_file} successfully!')
