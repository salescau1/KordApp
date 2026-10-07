import sqlite3
import re
import sys
import time
from pathlib import Path
from bs4 import BeautifulSoup
from collections import defaultdict

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.fetcher import fetch_html
from crawler.parsers.guitartuna import parse_gt_html
from crawler.batch_crawler import load_gt_artist_index, discover_gt_songs

TARGETS = [
    ("Plain White T's", "BARAT", "batch_08.db"),
    ("D'Masiv", "INDO", "batch_08.db"),
    ("Aretha Franklin", "BARAT", "batch_08.db"),
    ("Deep Purple", "BARAT", "batch_08.db"),
    ("Iron Maiden", "BARAT", "batch_08.db"),
    ("Sia", "BARAT", "batch_08.db"),
]

gt_index = load_gt_artist_index()

def main():
    for artist_name, region, batch_db_name in TARGETS:
        db_path = TOOLS_DIR / "catalog" / "db_batches" / batch_db_name
        gt_url = gt_index.get(artist_name.lower().strip())
        print(f"\nChecking GT for {artist_name}: URL = {gt_url}")
        if not gt_url:
            print("  No URL found in GT index!")
            continue

        gt_targets = discover_gt_songs(artist_name, gt_url)
        print(f"  Discovered {len(gt_targets)} GT song versions.")

if __name__ == "__main__":
    main()
