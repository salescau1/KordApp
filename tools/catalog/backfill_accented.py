import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.batch_crawler import crawl_and_build

ACCENTED_ARTISTS = [
    ("Michael Bublé", "BARAT"),
    ("Beyoncé", "BARAT"),
    ("Mötley Crüe", "BARAT"),
    ("Motörhead", "BARAT"),
    ("Björk", "BARAT"),
    ("Sigur Rós", "BARAT"),
    ("José González", "BARAT"),
    ("Sinéad O'Connor", "BARAT"),
]

if __name__ == "__main__":
    out_db = str(TOOLS_DIR / "catalog" / "db_batches" / "batch_accented.db")
    print(f"=== Crawling Accented Artists ({len(ACCENTED_ARTISTS)} artists) -> {out_db} ===")
    crawl_and_build(ACCENTED_ARTISTS, out_db)
    print("Done crawling accented artists!")
