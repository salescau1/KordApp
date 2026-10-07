import csv
import sys
sys.path.insert(0, r'C:\KordApp-AGY\tools')
from crawler.fetcher import fetch_html
import urllib.parse
import re, json, html

sys.stdout.reconfigure(encoding='utf-8')

candidates = [
    # Indo
    ("Last Child", "INDO"),
    ("Naff", "INDO"),
    ("Endank Soekamti", "INDO"),
    ("Shaggydog", "INDO"),
    ("Souljah", "INDO"),
    ("Danilla", "INDO"),
    ("Vierratale", "INDO"),
    ("Govinda", "INDO"),
    ("Setia Band", "INDO"),
    ("Ten2Five", "INDO"),
    ("Nike Ardilla", "INDO"),
    ("Poppy Mercury", "INDO"),
    ("Rio Febrian", "INDO"),
    ("The Overtunes", "INDO"),
    ("White Shoes & The Couples Company", "INDO"),
    ("Fabio Asher", "INDO"),
    ("Keisya Levronka", "INDO"),
    ("D'Lloyd", "INDO"),
    ("NDX A.K.A", "INDO"),
    ("GAC", "INDO"),
    ("Stars and Rabbit", "INDO"),
    ("Morfem", "INDO"),
    ("Scaller", "INDO"),
    ("Burgerkill", "INDO"),
    ("Goodnight Electric", "INDO"),
    ("Gugun Blues Shelter", "INDO"),
    ("The S.I.G.I.T.", "INDO"),
    ("Eleventwelfth", "INDO"),
    ("Sajama Cut", "INDO"),
    ("Diskoria", "INDO"),
    ("Gangga", "INDO"),
    ("Fariz RM", "INDO"),
    # Barat
    ("The Script", "BARAT"),
    ("Alter Bridge", "BARAT"),
    ("Jimi Hendrix", "BARAT"),
    ("Tears For Fears", "BARAT"),
    ("Justin Timberlake", "BARAT"),
    ("Ellie Goulding", "BARAT"),
    ("Meghan Trainor", "BARAT"),
    ("The Verve", "BARAT"),
    ("New Order", "BARAT"),
    ("Joy Division", "BARAT"),
    ("Franz Ferdinand", "BARAT"),
    ("Interpol", "BARAT"),
    ("Two Door Cinema Club", "BARAT"),
    ("Vampire Weekend", "BARAT"),
    ("MGMT", "BARAT"),
    ("Mac DeMarco", "BARAT"),
    ("Rex Orange County", "BARAT"),
    ("Men I Trust", "BARAT"),
    ("Wallows", "BARAT"),
    ("Keshi", "BARAT"),
    ("Boy Pablo", "BARAT"),
    ("Dean Lewis", "BARAT"),
    ("KISS", "BARAT"),
    ("Genesis", "BARAT"),
    ("Alice Cooper", "BARAT"),
    ("Chuck Berry", "BARAT"),
    ("The Everly Brothers", "BARAT"),
    ("Tom Jones", "BARAT"),
    ("Bonnie Tyler", "BARAT"),
    ("The Stone Roses", "BARAT"),
    ("Supergrass", "BARAT"),
    ("The Libertines", "BARAT"),
    ("Kasabian", "BARAT"),
    ("Mayday Parade", "BARAT"),
    ("Sheryl Crow", "BARAT"),
    ("Dave Matthews Band", "BARAT"),
    ("Status Quo", "BARAT"),
    ("Chris Norman", "BARAT"),
    ("Morrissey", "BARAT"),
    ("The Hollies", "BARAT"),
    ("Manic Street Preachers", "BARAT"),
    ("Cheap Trick", "BARAT"),
    ("Beck", "BARAT"),
    ("McFly", "BARAT"),
    ("Jethro Tull", "BARAT"),
    ("Donovan", "BARAT"),
    ("Biffy Clyro", "BARAT"),
    ("The Monkees", "BARAT"),
    ("The Byrds", "BARAT"),
    ("T. Rex", "BARAT"),
    ("Dropkick Murphys", "BARAT"),
    ("The National", "BARAT"),
    ("Carrie Underwood", "BARAT"),
    ("REO Speedwagon", "BARAT"),
    ("Sleeping at Last", "BARAT"),
    ("Take That", "BARAT"),
    ("Cliff Richard", "BARAT"),
    ("Arcade Fire", "BARAT"),
    ("Phoenix", "BARAT"),
    ("Passion Pit", "BARAT"),
    ("Crowded House", "BARAT"),
    ("Men At Work", "BARAT"),
    ("INXS", "BARAT"),
    ("Midnight Oil", "BARAT"),
    ("Simple Minds", "BARAT"),
    ("Echo & The Bunnymen", "BARAT")
]

print(f"Total candidates to test on UG: {len(candidates)}")
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

for name, reg in candidates[:15]:
    cnt = check_ug_count(name)
    print(f"[{reg}] {name:<30} -> {cnt} chords found on page 1 of UG")
