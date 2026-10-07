import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.batch_crawler import crawl_and_build

BATCH_21_ARTISTS = [
    # 🪩 Disco, Funk, Party & Retro Dance Anthems
    ("Village People", "BARAT"),
    ("Gloria Gaynor", "BARAT"),
    ("Cake", "BARAT"),
    ("Boney M.", "BARAT"),
    ("Donna Summer", "BARAT"),
    ("Lipps Inc.", "BARAT"),
    ("The Trammps", "BARAT"),
    ("Sister Sledge", "BARAT"),
    ("Chaka Khan", "BARAT"),
    ("The Pointer Sisters", "BARAT"),
    ("Rick James", "BARAT"),
    ("Wild Cherry", "BARAT"),
    ("The Weather Girls", "BARAT"),
    ("Carl Douglas", "BARAT"),
    ("Hot Chocolate", "BARAT"),
    ("War", "BARAT"),
    ("The Gap Band", "BARAT"),
    ("Ottawan", "BARAT"),
    ("Baccara", "BARAT"),
    ("A Taste of Honey", "BARAT"),

    # 🎹 Euro-Disco 80-an Legendaris
    ("Modern Talking", "BARAT"),
    ("Bad Boys Blue", "BARAT"),
    ("Gazebo", "BARAT"),
    ("C.C. Catch", "BARAT"),

    # 🎸 2000s Post-Grunge, Alt-Rock & Radio Anthems
    ("Seether", "BARAT"),
    ("Breaking Benjamin", "BARAT"),
    ("Three Days Grace", "BARAT"),
    ("Hinder", "BARAT"),
    ("Puddle of Mudd", "BARAT"),
    ("Crossfade", "BARAT"),
    ("Trapt", "BARAT"),
    ("Chevelle", "BARAT"),
    ("Finger Eleven", "BARAT"),

    # 📻 Viral Pop-Acoustic & Modern Indie Ballads
    ("New West", "BARAT"),
    ("Henry Moodie", "BARAT"),
    ("Matt Maltese", "BARAT"),
    ("Alexander 23", "BARAT"),
]

if __name__ == "__main__":
    out_db = str(TOOLS_DIR / "catalog" / "db_batches" / "batch_21.db")
    print(f"=== Crawling Batch 21 ({len(BATCH_21_ARTISTS)} artists) -> {out_db} ===")
    crawl_and_build(BATCH_21_ARTISTS, out_db)
    print("Done crawling batch 21!")
