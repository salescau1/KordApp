import sys
from pathlib import Path
sys.path.insert(0, 'tools')
from crawler.batch_crawler import discover_ug_songs, load_gt_artist_index

gt = load_gt_artist_index()

disney_candidates = [
    ("Mandy Moore", "BARAT", "USA", "pop|soundtrack", "2000s"),
    ("Randy Newman", "BARAT", "USA", "soundtrack|folk", "1970s"),
    ("Michael Bolton", "BARAT", "USA", "pop rock|soundtrack", "1980s"),
    ("Vanessa Williams", "BARAT", "USA", "pop|r&b|soundtrack", "1990s"),
    ("Auli'i Cravalho", "BARAT", "USA", "soundtrack|pop", "2010s"),
    ("Anthony Gonzalez", "BARAT", "USA", "soundtrack|pop", "2010s"),
    ("Lin-Manuel Miranda", "BARAT", "USA", "soundtrack|musicals", "2010s"),
    ("Lea Salonga", "ASIA", "Philippines", "soundtrack|broadway", "1990s"),
]

for name, region, country, genre, era in disney_candidates:
    in_gt = name.lower().strip() in gt
    ug = discover_ug_songs(name, max_pages=1)
    print(f"Disney candidate: {name} -> GT={in_gt}, UG={len(ug)} tabs")
