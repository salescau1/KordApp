import csv
import glob
import re
import sys
import unicodedata
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.batch_crawler import load_gt_artist_index, discover_ug_songs

GT_INDEX = load_gt_artist_index()

# Candidate list for Batch 18: The Ultimate OST & Soundtrack Edition
CANDIDATES = [
    # --- ANIME & JAPAN SOUNDTRACK ---
    ("Akeboshi", "ASIA", "Japan", "j-pop|folk|anime", "2000s"),
    ("Ikimonogakari", "ASIA", "Japan", "j-pop|j-rock|anime", "2000s"),
    ("Koji Wada", "ASIA", "Japan", "anime|j-rock", "2000s"),
    ("Field of View", "ASIA", "Japan", "j-rock|anime", "1990s"),
    ("Linked Horizon", "ASIA", "Japan", "symphonic rock|anime", "2010s"),
    ("TK from Ling Tosite Sigure", "ASIA", "Japan", "post-hardcore|anime", "2010s"),
    ("Ai Higuchi", "ASIA", "Japan", "j-pop|anime", "2020s"),
    ("Hironobu Kageyama", "ASIA", "Japan", "anime|hard rock", "1980s"),
    ("Do As Infinity", "ASIA", "Japan", "j-rock|pop rock", "2000s"),
    ("Orange Range", "ASIA", "Japan", "j-rock|pop rock", "2000s"),
    ("Motohiro Hata", "ASIA", "Japan", "j-pop|acoustic", "2010s"),
    ("7!!", "ASIA", "Japan", "j-pop|anime", "2010s"),
    ("Hiroshi Kitadani", "ASIA", "Japan", "anime|j-rock", "1990s"),
    ("Porno Graffitti", "ASIA", "Japan", "j-rock|pop rock", "2000s"),
    ("SID", "ASIA", "Japan", "j-rock|visual kei", "2000s"),
    ("DA PUMP", "ASIA", "Japan", "j-pop|dance", "2000s"),
    ("Hiroyuki Okita", "ASIA", "Japan", "j-pop|anime", "1980s"),
    ("FIELD OF VIEW", "ASIA", "Japan", "j-rock|anime", "1990s"),

    # --- INDO FILM, SINETRON & OST ---
    ("Sherina Munaf", "INDO", "Indonesia", "pop|soundtrack", "2000s"),
    ("Acha Septriasa", "INDO", "Indonesia", "pop|soundtrack", "2000s"),
    ("Irwansyah", "INDO", "Indonesia", "pop|soundtrack", "2000s"),
    ("The Panasdalam Bank", "INDO", "Indonesia", "folk|indie|soundtrack", "2010s"),
    ("Anda Perdana", "INDO", "Indonesia", "indie rock|soundtrack", "2000s"),
    ("Rano Karno", "INDO", "Indonesia", "pop|soundtrack", "1980s"),
    ("Ipang", "INDO", "Indonesia", "rock|pop rock|soundtrack", "2000s"),
    ("Ade Govinda", "INDO", "Indonesia", "pop|soundtrack", "2020s"),
    ("Wizzy", "INDO", "Indonesia", "pop|r&b|soundtrack", "2010s"),
    ("Hanin Dhiya", "INDO", "Indonesia", "pop|soundtrack", "2010s"),
    ("Iqbaal Ramadhan", "INDO", "Indonesia", "pop|soundtrack", "2010s"),
    ("Eross Candra", "INDO", "Indonesia", "rock|soundtrack", "2000s"),
    ("Amigdala", "INDO", "Indonesia", "indie folk|soundtrack", "2010s"),

    # --- K-DRAMA OST ---
    ("Chanyeol", "ASIA", "South Korea", "k-pop|ost", "2010s"),
    ("Punch", "ASIA", "South Korea", "k-pop|ost", "2010s"),
    ("Crush", "ASIA", "South Korea", "r&b|k-pop|ost", "2010s"),
    ("Ailee", "ASIA", "South Korea", "k-pop|ost", "2010s"),
    ("Davichi", "ASIA", "South Korea", "k-pop|ballad|ost", "2010s"),
    ("Gummy", "ASIA", "South Korea", "k-pop|ballad|ost", "2000s"),
    ("Lee Juck", "ASIA", "South Korea", "folk|k-pop|ost", "2010s"),
    ("Gaho", "ASIA", "South Korea", "k-pop|ost", "2020s"),
    ("Taeyeon", "ASIA", "South Korea", "k-pop|ballad|ost", "2010s"),
    ("Lyn", "ASIA", "South Korea", "k-pop|ballad|ost", "2000s"),
    ("Kim Hyun Joong", "ASIA", "South Korea", "k-pop|ost", "2000s"),
    ("Byul", "ASIA", "South Korea", "k-pop|ballad|ost", "2000s"),

    # --- HOLLYWOOD, DISNEY & SERIES SOUNDTRACK ---
    ("Idina Menzel", "BARAT", "USA", "soundtrack|broadway|pop", "2010s"),
    ("Keala Settle", "BARAT", "USA", "soundtrack|pop", "2010s"),
    ("Loren Allred", "BARAT", "USA", "soundtrack|pop", "2010s"),
    ("Hugh Jackman", "BARAT", "Australia", "soundtrack|musicals", "2010s"),
    ("Naomi Scott", "BARAT", "UK", "soundtrack|pop", "2010s"),
    ("Zendaya", "BARAT", "USA", "pop|soundtrack", "2010s"),
    ("Phil Collins", "BARAT", "UK", "pop rock|soundtrack", "1980s"),
    ("Kenny Loggins", "BARAT", "USA", "rock|soundtrack", "1980s"),
    ("Ronan Keating", "BARAT", "Ireland", "pop|soundtrack", "1990s"),
    ("Elvis Costello", "BARAT", "UK", "new wave|soundtrack", "1970s"),
    ("Wiz Khalifa", "BARAT", "USA", "hip-hop|soundtrack", "2010s"),
    ("Chad Kroeger", "BARAT", "Canada", "post-grunge|soundtrack", "2000s"),
    ("Berlin", "BARAT", "USA", "synthpop|soundtrack", "1980s"),
    ("Survivor", "BARAT", "USA", "hard rock|soundtrack", "1980s"),
    ("Ray Parker Jr.", "BARAT", "USA", "pop|r&b|soundtrack", "1980s"),
    ("Huey Lewis and the News", "BARAT", "USA", "pop rock|soundtrack", "1980s"),
    ("Steppenwolf", "BARAT", "USA", "hard rock|soundtrack", "1960s"),
    ("Peabo Bryson", "BARAT", "USA", "r&b|soundtrack", "1990s"),
]

def to_slug(text: str) -> str:
    s = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode('utf-8')
    s = re.sub(r'[^a-zA-Z0-9]+', '-', s).strip('-').lower()
    return s

def main():
    existing_artists = set()
    for f in glob.glob("tools/catalog/batches/batch_*.csv"):
        with open(f, encoding="utf-8") as fp:
            for row in csv.DictReader(fp):
                existing_artists.add(row["artist"].strip().lower())
                existing_artists.add(row["artist_slug"].strip().lower())

    verified_batch = []
    seen = set()

    print(f"Auditing {len(CANDIDATES)} candidates for Batch 18...")

    for artist, region, country, genre, era in CANDIDATES:
        slug = to_slug(artist)
        if slug in seen:
            continue
        seen.add(slug)

        if artist.lower() in existing_artists or slug in existing_artists:
            print(f"  [SKIP] Already in previous batch: {artist}")
            continue

        in_gt = artist.lower().strip() in GT_INDEX
        try:
            ug_songs = discover_ug_songs(artist, max_pages=1)
            ug_count = len(ug_songs)
        except Exception:
            ug_count = 0

        if in_gt or ug_count > 0:
            est_songs = max(10, min(100, ug_count if ug_count > 0 else 15))
            verified_batch.append({
                "artist": artist,
                "region": region,
                "country": country,
                "genre": genre,
                "era": era,
                "lagu": est_songs,
                "artist_slug": slug
            })
            print(f"  [ADD] {artist} (GT={in_gt}, UG={ug_count}) -> {est_songs} lagu")
        else:
            print(f"  [NO TABS] {artist}")

    out_file = Path("tools/catalog/batches/batch_18.csv")
    with open(out_file, "w", newline="", encoding="utf-8") as f:
        fieldnames = ["artist", "region", "country", "genre", "era", "lagu", "artist_slug"]
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in verified_batch:
            writer.writerow(row)

    print(f"\nSuccessfully created {out_file} with {len(verified_batch)} artists!")

if __name__ == "__main__":
    main()
