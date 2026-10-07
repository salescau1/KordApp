import html
import json
import re
import sys
import time
import urllib.parse
import urllib.request

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

headers = {'User-Agent': 'Mozilla/5.0'}

QUERIES = [
    ("Dua Lipa Dance The Night", "Dance The Night"),
    ("Phil Collins You'll Be In My Heart", "You'll Be In My Heart"),
    ("Phil Collins Strangers Like Me", "Strangers Like Me"),
    ("Camp Rock Gotta Find You", "Gotta Find You"),
    ("Demi Lovato This Is Me", "This Is Me"),
    ("Lady Gaga Hold My Hand", "Hold My Hand"),
    ("Lady Gaga Always Remember Us This Way", "Always Remember Us This Way"),
    ("Descendants of the Sun This Love", "This Love"),
    ("Descendants of the Sun Everytime", "Everytime"),
    ("Descendants of the Sun You Are My Everything", "You Are My Everything"),
    ("Crash Landing on You Sunset", "Sunset"),
    ("Crash Landing on You Give You My Heart", "Give You My Heart"),
    ("Hotel Del Luna All About You", "All About You"),
    ("Hotel Del Luna Remember Me", "Remember Me"),
    ("Reply 1988 Don't Worry", "Don't Worry"),
    ("Sherina Lihatlah", "Lihatlah"),
    ("Sherina Menikmati Hari", "Menikmati"),
    ("Sherina Bintang", "Bintang"),
    ("Sherina Persahabatan", "Persahabatan"),
    ("Acha Septriasa Berdua Lebih Baik", "Berdua"),
    ("Bunga Citra Lestari Harta Berharga", "Harta Berharga"),
    ("Si Doel Anak Sekolahan", "Si Doel"),
    ("Wizzy Selamat Jalan Kekasih", "Selamat Jalan Kekasih"),
    ("SIAM SHADE 1/3", "1/3"),
    ("Kera Sakti", "Kera Sakti"),
]

for q, target in QUERIES:
    url = f"https://www.ultimate-guitar.com/search.php?title={urllib.parse.quote(q)}&type=300"
    req = urllib.request.Request(url, headers=headers)
    try:
        content = urllib.request.urlopen(req, timeout=10).read().decode('utf-8', errors='replace')
        m = re.search(r'class="js-store"\s+data-content="([^"]+)"', content)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
            chord_results = [r for r in results if r.get('type') == 'Chords']
            if chord_results:
                top = chord_results[0]
                print(f"FOUND: '{q}' -> {top.get('artist_name')} - {top.get('song_name')} (votes {top.get('votes')}): {top.get('tab_url')}", flush=True)
            else:
                print(f"NO CHORDS: '{q}'", flush=True)
        else:
            print(f"NO DATA: '{q}'", flush=True)
    except Exception as e:
        print(f"ERR '{q}': {e}", flush=True)
    time.sleep(0.3)
