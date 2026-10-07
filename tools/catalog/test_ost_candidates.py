import sys
from pathlib import Path
sys.path.insert(0, 'tools')
from crawler.batch_crawler import discover_ug_songs, load_gt_artist_index

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

gt = load_gt_artist_index()

candidates = [
    # Anime & Japan OST
    ("Akeboshi", "ASIA", "Japan", "j-pop|folk|anime", "2000s"),
    ("Ikimonogakari", "ASIA", "Japan", "j-pop|j-rock|anime", "2000s"),
    ("Koji Wada", "ASIA", "Japan", "anime|j-rock", "2000s"),
    ("Field of View", "ASIA", "Japan", "j-rock|anime", "1990s"),
    ("Linked Horizon", "ASIA", "Japan", "symphonic rock|anime", "2010s"),
    ("YUI", "ASIA", "Japan", "j-pop|j-rock|anime", "2000s"),
    ("TK from Ling Tosite Sigure", "ASIA", "Japan", "post-hardcore|anime", "2010s"),
    ("Ai Higuchi", "ASIA", "Japan", "j-pop|anime", "2020s"),
    ("Field of View", "ASIA", "Japan", "j-rock|anime", "1990s"),
    ("Ayumi Miyazaki", "ASIA", "Japan", "j-rock|anime", "2000s"),
    ("Hironobu Kageyama", "ASIA", "Japan", "anime|hard rock", "1980s"),
    ("Do As Infinity", "ASIA", "Japan", "j-rock|pop rock", "2000s"),
    ("Orange Range", "ASIA", "Japan", "j-rock|pop rock", "2000s"),
    ("Motohiro Hata", "ASIA", "Japan", "j-pop|acoustic", "2010s"),
    ("7!!", "ASIA", "Japan", "j-pop|anime", "2010s"),

    # Indo OST / Film / Sinetron
    ("Sherina", "INDO", "Indonesia", "pop|soundtrack", "2000s"),
    ("Sherina Munaf", "INDO", "Indonesia", "pop|soundtrack", "2000s"),
    ("Acha Septriasa", "INDO", "Indonesia", "pop|soundtrack", "2000s"),
    ("Irwansyah", "INDO", "Indonesia", "pop|soundtrack", "2000s"),
    ("The Panasdalam Bank", "INDO", "Indonesia", "folk|indie|soundtrack", "2010s"),
    ("Anda Perdana", "INDO", "Indonesia", "indie rock|soundtrack", "2000s"),
    ("Anda Bunga", "INDO", "Indonesia", "indie rock|soundtrack", "2000s"),
    ("Rano Karno", "INDO", "Indonesia", "pop|soundtrack", "1980s"),
    ("Amigdala", "INDO", "Indonesia", "indie folk|soundtrack", "2010s"),
    ("Ipang", "INDO", "Indonesia", "rock|pop rock|soundtrack", "2000s"),
    ("Ade Govinda", "INDO", "Indonesia", "pop|soundtrack", "2020s"),

    # K-Drama OST
    ("Chanyeol", "ASIA", "South Korea", "k-pop|ost", "2010s"),
    ("Punch", "ASIA", "South Korea", "k-pop|ost", "2010s"),
    ("Crush", "ASIA", "South Korea", "r&b|k-pop|ost", "2010s"),
    ("Ailee", "ASIA", "South Korea", "k-pop|ost", "2010s"),
    ("Davichi", "ASIA", "South Korea", "k-pop|ballad|ost", "2010s"),
    ("Gummy", "ASIA", "South Korea", "k-pop|ballad|ost", "2000s"),
    ("Lee Juck", "ASIA", "South Korea", "folk|k-pop|ost", "2010s"),
    ("Yoon Mi-rae", "ASIA", "South Korea", "k-pop|r&b|ost", "2010s"),
    ("Jo Jung Suk", "ASIA", "South Korea", "k-pop|ost", "2020s"),
    ("Gaho", "ASIA", "South Korea", "k-pop|ost", "2020s"),

    # Hollywood / Disney / Western OST
    ("Idina Menzel", "BARAT", "USA", "soundtrack|broadway|pop", "2010s"),
    ("Keala Settle", "BARAT", "USA", "soundtrack|pop", "2010s"),
    ("Loren Allred", "BARAT", "USA", "soundtrack|pop", "2010s"),
    ("Benj Pasek and Justin Paul", "BARAT", "USA", "soundtrack|musicals", "2010s"),
    ("Hugh Jackman", "BARAT", "Australia", "soundtrack|musicals", "2010s"),
    ("Naomi Scott", "BARAT", "UK", "soundtrack|pop", "2010s"),
    ("Zendaya", "BARAT", "USA", "pop|soundtrack", "2010s"),
    ("Phil Collins", "BARAT", "UK", "pop rock|soundtrack", "1980s"),
    ("Kenny Loggins", "BARAT", "USA", "rock|soundtrack", "1980s"),
    ("Aerosmith", "BARAT", "USA", "hard rock|soundtrack", "1970s"),
    ("Chad Kroeger", "BARAT", "Canada", "post-grunge|soundtrack", "2000s"),
]

print(f"Testing {len(candidates)} candidates...")
results = []
for name, region, country, genre, era in candidates:
    in_gt = name.lower().strip() in gt
    ug_list = discover_ug_songs(name, max_pages=1)
    ug_cnt = len(ug_list)
    print(f"[{'OK' if (in_gt or ug_cnt > 0) else 'MISS'}] {name}: GT={in_gt}, UG={ug_cnt}")
    if in_gt or ug_cnt > 0:
        results.append((name, region, country, genre, era, ug_cnt, in_gt))

print(f"\nTotal viable artists: {len(results)}/{len(candidates)}")
