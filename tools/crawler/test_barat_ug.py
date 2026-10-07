import sys
sys.path.insert(0, r'C:\KordApp-AGY\tools')
from crawler.fetcher import fetch_html
import urllib.parse
import re, json, html

barat_test = [
    "The Script", "Alter Bridge", "Jimi Hendrix", "Tears For Fears", "Justin Timberlake",
    "Ellie Goulding", "Meghan Trainor", "The Verve", "New Order", "Joy Division",
    "Franz Ferdinand", "Interpol", "Two Door Cinema Club", "Vampire Weekend", "MGMT",
    "Mac DeMarco", "Rex Orange County", "Men I Trust", "Wallows", "Keshi"
]

def check_ug_count(artist_name):
    encoded = urllib.parse.quote(artist_name)
    url = f"https://www.ultimate-guitar.com/search.php?title={encoded}&type=300"
    html_text = fetch_html(url)
    if not html_text:
        return 0
    m = re.search(r'class="js-store" data-content="([^"]+)"', html_text)
    if not m:
        return 0
    try:
        data = json.loads(html.unescape(m.group(1)))
        results = data.get("store", {}).get("page", {}).get("data", {}).get("results", [])
        chords = [r for r in results if r.get("type") == "Chords"]
        return len(chords)
    except:
        return 0

for name in barat_test:
    cnt = check_ug_count(name)
    print(f"[BARAT] {name:<25} -> {cnt} chords found on page 1 of UG")
