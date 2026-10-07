import csv
import glob
import re
import sys
import urllib.parse
from pathlib import Path
from bs4 import BeautifulSoup
import requests

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.fetcher import fetch_html

def test_artist_ug(artist_name):
    query = urllib.parse.quote(artist_name)
    url = f"https://www.ultimate-guitar.com/search.php?search_type=band&value={query}"
    html_content = fetch_html(url)
    if not html_content:
        return 0, ""
    soup = BeautifulSoup(html_content, "html.parser")
    store = soup.find("div", class_="js-store")
    if not store:
        return 0, ""
    import json
    try:
        data = json.loads(store.get("data-content", "{}"))
        results = data.get("store", {}).get("page", {}).get("data", {}).get("results", [])
        if results and isinstance(results, list):
            first = results[0]
            count = first.get("count", 0)
            band_url = first.get("artist_url", "")
            return count, band_url
    except Exception:
        pass
    return 0, ""

def main():
    # Load candidate artists
    existing = set()
    for f in glob.glob("tools/catalog/batches/batch_*.csv"):
        with open(f, encoding="utf-8") as fp:
            for row in csv.DictReader(fp):
                existing.add(row["artist"].strip().lower())

    p_id = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2136\content.md"
    p_gl = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2140\content.md"
    
    candidates = []
    
    def process_file(p_path, default_region):
        soup = BeautifulSoup(Path(p_path).read_text(encoding="utf-8", errors="ignore"), "html.parser")
        for tr in soup.find_all("tr"):
            td_title = tr.find("td", class_="text mp")
            if not td_title:
                continue
            art_tags = td_title.find_all("a", href=lambda h: h and "artist" in h)
            trk_tag = td_title.find("a", href=lambda h: h and "track" in h)
            if art_tags and trk_tag:
                trk = trk_tag.get_text().strip()
                for a in art_tags:
                    name = a.get_text().strip()
                    if name and name.lower() not in existing:
                        candidates.append((name, default_region, trk))

    process_file(p_id, "INDO")
    process_file(p_gl, "BARAT")

    # Filter junk/movie/generic names
    blacklist = {
        "the hunger games", "solto (fr)", "copacabana", "muslih fvnky", "mingse",
        "akbar chalay", "hugel", "gener8ion", "yung lean", "kyla", "nayer", "solto",
        "ultra naté", "imael angel", "huntr/x"
    }

    unique_candidates = []
    seen = set()
    for name, reg, trk in candidates:
        low = name.lower()
        if low in seen or low in blacklist or len(name) < 2:
            continue
        seen.add(low)
        unique_candidates.append((name, reg, trk))

    print(f"Testing {len(unique_candidates)} curated candidates against UG...")
    valid_artists = []
    for name, reg, trk in unique_candidates:
        cnt, url = test_artist_ug(name)
        if cnt > 0:
            print(f"  [FOUND {cnt:3d} songs] {name} ({reg}) -> {url}")
            valid_artists.append({
                "artist": name,
                "region": reg,
                "country": "Indonesia" if reg == "INDO" else ("United States" if reg == "BARAT" else "South Korea"),
                "genre": "pop|rock" if reg == "INDO" else "pop|alternative rock",
                "era": "2020s",
                "lagu": cnt,
                "artist_slug": re.sub(r"[^a-zA-Z0-9]+", "-", name.lower()).strip("-")
            })
        else:
            print(f"  [NOT FOUND / 0 songs] {name} ({reg})")

    print(f"\nTotal valid artists with tabs on UG: {len(valid_artists)}")

if __name__ == "__main__":
    main()
