import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.batch_crawler import crawl_and_build

BATCH_19_ARTISTS = [
    ("Owl City", "BARAT"),
    ("Carly Rae Jepsen", "BARAT"),
    ("Laufey", "BARAT"),
    ("Glass Animals", "BARAT"),
    ("fun.", "BARAT"),
    ("Walk The Moon", "BARAT"),
    ("Of Monsters and Men", "BARAT"),
    ("Gotye", "BARAT"),
    ("Milky Chance", "BARAT"),
    ("MAGIC!", "BARAT"),
    ("Portugal. The Man", "BARAT"),
    ("Grouplove", "BARAT"),
    ("American Authors", "BARAT"),
    ("Neon Trees", "BARAT"),
    ("Capital Cities", "BARAT"),
    ("Fitz and the Tantrums", "BARAT"),
    ("Young the Giant", "BARAT"),
    ("BØRNS", "BARAT"),
    ("Jeremy Zucker", "BARAT"),
    ("Ruth B.", "BARAT"),
    ("d4vd", "BARAT"),
    ("dhruv", "BARAT"),
    ("Phum Viphurit", "BARAT"),
    ("Cavetown", "BARAT"),
    ("Ricky Montgomery", "BARAT"),
    ("The Postal Service", "BARAT"),
    ("Rachel Platten", "BARAT"),
    ("MKTO", "BARAT"),
    ("Saint Motel", "BARAT"),
    ("The Naked and Famous", "BARAT"),
    ("Grover Washington, Jr.", "BARAT"),
]

if __name__ == "__main__":
    out_db = str(TOOLS_DIR / "catalog" / "db_batches" / "batch_19.db")
    print(f"=== Crawling Batch 19 ({len(BATCH_19_ARTISTS)} artists) -> {out_db} ===")
    crawl_and_build(BATCH_19_ARTISTS, out_db)
    print("Done crawling batch 19!")
