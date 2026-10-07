import csv
import glob
import re
import sys
import unicodedata
from pathlib import Path
from bs4 import BeautifulSoup

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.batch_crawler import load_gt_artist_index, discover_ug_songs

GT_INDEX = load_gt_artist_index()

def clean_artist_name(name: str) -> str:
    name = unicodedata.normalize('NFKD', name).encode('ascii', 'ignore').decode('utf-8')
    name = re.sub(r"\s+", " ", name).strip()
    return name

# Alias mapping for known discrepancies
ALIASES = {
    "the goo goo dolls": "Goo Goo Dolls",
    "andra & the backbone": "Andra and The Backbone",
    "enau": "Enau",
    "macklemore & ryan lewis": "Macklemore",
    "the chainsmokers": "The Chainsmokers",
}

def check_artist_available(artist_name: str) -> tuple[bool, int, str]:
    # Check alias first
    norm_low = artist_name.lower().strip()
    target_name = ALIASES.get(norm_low, artist_name)
    
    # 1. Check GT index
    in_gt = target_name.lower().strip() in GT_INDEX
    
    # 2. Check UG
    try:
        ug_songs = discover_ug_songs(target_name, max_pages=1)
        ug_count = len(ug_songs)
    except Exception:
        ug_count = 0
        
    has_songs = in_gt or ug_count > 0
    return has_songs, ug_count, target_name

def main():
    existing = set()
    for f in glob.glob("tools/catalog/batches/batch_*.csv"):
        with open(f, encoding="utf-8") as fp:
            for row in csv.DictReader(fp):
                existing.add(row["artist"].strip().lower())

    p_id = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2136\content.md"
    p_gl = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2140\content.md"
    p_ls = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2142\content.md"

    # Specific lists from Spotify Charts
    indo_raw = [
        "Raim Laode", "Enau", "Ari Lesmana", "Perunggu", "Monita Tahalea",
        "Andmesh", "Andra and The Backbone", "Batas Senja", "Mahen", "MASDO",
        "Suara Kayu", "The Virgin", "Utopia", "Adrian Khalif", "The Lantis",
        "Daun Jatuh", "Raissa Anggiani", "Yovie Widianto", "Dongker", "Jason Ranti",
        "Fatin", "Mikha Angelo", "Kaleb J", "Dendi Nata", "Stereo Wall"
    ]

    barat_raw = [
        "Goo Goo Dolls", "Tame Impala", "The Neighbourhood", "Mac Miller", "Daniel Caesar",
        "Drake", "Bad Bunny", "Macklemore", "TLC", "Pitbull", "Jennifer Lopez",
        "Ne-Yo", "Daft Punk", "Marshmello", "Khalid", "Djo", "Lord Huron",
        "Steve Lacy", "girl in red", "Zara Larsson", "Bleachers", "Sam Fender",
        "Gorillaz", "Mazzy Star", "The Chainsmokers", "Empire of the Sun", "Kali Uchis",
        "Burna Boy", "Don Toliver", "RAYE", "Alex Warren", "Ella Langley", "Tems", "Miguel"
    ]

    asia_raw = [
        "JENNIE", "ROSE", "TOMORROW X TOGETHER", "FIFTY FIFTY", "Hikaru Utada", "back number"
    ]

    all_candidates = []
    for a in indo_raw:
        all_candidates.append((a, "INDO", "Indonesia", "pop|pop rock|indie", "2020s"))
    for a in barat_raw:
        all_candidates.append((a, "BARAT", "United States", "pop|alternative rock|indie rock", "2010s"))
    for a in asia_raw:
        reg = "JAPAN" if a in ["Hikaru Utada", "back number"] else "KOREA"
        cntry = "Japan" if reg == "JAPAN" else "South Korea"
        all_candidates.append((a, reg, cntry, "k-pop|j-pop|pop", "2020s"))

    verified = []
    print(f"Verifying {len(all_candidates)} curated candidates...")
    for art, reg, cntry, genre, era in all_candidates:
        clean_name = clean_artist_name(art)
        if clean_name.lower() in existing:
            print(f"  [SKIP - already exists]: {clean_name}")
            continue
            
        avail, cnt, effective_name = check_artist_available(clean_name)
        if avail:
            slug = re.sub(r"[^a-zA-Z0-9]+", "-", effective_name.lower()).strip("-")
            print(f"  [OK] {effective_name} ({reg}) - UG songs found: {cnt}, in GT: {effective_name.lower() in GT_INDEX}")
            verified.append({
                "artist": effective_name,
                "region": reg,
                "country": cntry,
                "genre": genre,
                "era": era,
                "lagu": max(cnt * 5, 20),
                "artist_slug": slug
            })
        else:
            print(f"  [FAIL - no tabs]: {clean_name}")

    out_csv = TOOLS_DIR / "catalog" / "batches" / "batch_16.csv"
    with open(out_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["artist", "region", "country", "genre", "era", "lagu", "artist_slug"])
        writer.writeheader()
        writer.writerows(verified)

    print(f"\nSuccessfully wrote {len(verified)} artists to {out_csv}!")

if __name__ == "__main__":
    main()
