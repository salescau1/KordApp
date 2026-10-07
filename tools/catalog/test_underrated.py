import sqlite3
import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.batch_crawler import load_gt_artist_index, discover_ug_songs

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()
gt = load_gt_artist_index()

test_list = [
    "D'Paspor", "Wayang", "Juliette", "Vagetoz", "Goliath", "Tiket", "Zivilia", "SouQy",
    "Bagindas", "Tahta", "Nano", "Matta", "Lobow", "Closehead", "Captain Jack",
    "Rocket Rockers", "Superglad", "Killing Me Inside", "Pee Wee Gaskins", "Garasi",
    "Saint Loco", "Base Jam", "Bragi", "Protonema", "Dr. PM", "Bunglon", "Kahitna", "Kerispatih", "Naff"
]

print(f"{'Artist':20s} | {'In DB':5s} | {'In GT':5s} | {'UG Songs':8s}")
print("-" * 50)
for name in test_list:
    slug = name.lower().replace("'", "").replace(".", "").replace(" ", "-")
    in_db = c.execute("SELECT count(*) FROM songs WHERE lower(artist) = ? OR artist_slug LIKE ?", (name.lower(), f"%{slug}%")).fetchone()[0]
    in_gt = name.lower() in gt or name.lower().replace("'", "") in gt
    try:
        ug_cnt = len(discover_ug_songs(name, max_pages=1))
    except Exception:
        ug_cnt = 0
    print(f"{name:20s} | {in_db:5d} | {str(in_gt):5s} | {ug_cnt:8d}")
