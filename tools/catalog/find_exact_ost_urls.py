import html
import json
import re
import sys
import time
import urllib.parse
import urllib.request

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

def search_ug(q):
    url = f"https://www.ultimate-guitar.com/search.php?title={urllib.parse.quote(q)}&type=300"
    req = urllib.request.Request(url, headers=headers)
    try:
        content = urllib.request.urlopen(req, timeout=10).read().decode('utf-8', errors='replace')
        m = re.search(r'class="js-store"\s+data-content="([^"]+)"', content)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
            return [r for r in results if r.get('type') == 'Chords']
    except Exception as e:
        # print(f"Error {q}: {e}")
        pass
    return []

# Test queries for all desired tracks:
TRACKS = [
    # Anime
    ("One Piece We Are", "We Are"),
    ("One Piece Kokoro no Chizu", "Kokoro no Chizu"),
    ("One Piece Memories", "Memories"),
    ("One Piece New Genesis", "New Genesis"),
    ("Digimon Butter-Fly", "Butter-Fly"),
    ("Digimon Brave Heart", "Brave Heart"),
    ("Dragon Ball Dan Dan", "Dan Dan"),
    ("Dragon Ball Cha-La", "Cha-La"),
    ("Tokyo Ghoul Unravel", "Unravel"),
    ("Inuyasha Every Heart", "Every Heart"),
    ("Doraemon no Uta", "Doraemon"),
    ("Rurouni Kenshin 1/3", "1/3"),
    ("Rurouni Kenshin Heart of Sword", "Heart of Sword"),
    ("Sobakasu Judy and Mary", "Sobakasu"),
    ("Slam Dunk Kimi ga Suki", "Kimi ga Suki"),
    ("Slam Dunk Sekai ga Owaru", "Sekai ga Owaru"),
    ("Slam Dunk My Friend", "My Friend"),
    ("Detective Conan Secret of My Heart", "Secret of My Heart"),
    ("Yu Yu Hakusho Hohoemi no Bakudan", "Hohoemi"),

    # Disney & Western
    ("Idina Menzel Let It Go", "Let It Go"),
    ("Idina Menzel Into The Unknown", "Into The Unknown"),
    ("Aladdin A Whole New World", "A Whole New World"),
    ("Tangled I See The Light", "I See The Light"),
    ("Tangled When Will My Life Begin", "When Will My Life Begin"),
    ("Toy Story You've Got A Friend In Me", "Friend In Me"),
    ("Moana How Far I'll Go", "How Far"),
    ("Encanto We Don't Talk About Bruno", "Bruno"),
    ("Hercules Go The Distance", "Go The Distance"),
    ("Pocahontas Colors Of The Wind", "Colors Of The Wind"),
    ("Coco Remember Me", "Remember Me"),
    ("Tarzan You'll Be In My Heart", "You'll Be In My Heart"),
    ("High School Musical Breaking Free", "Breaking Free"),
    ("High School Musical Start of Something New", "Start of Something New"),
    ("Camp Rock This Is Me", "This Is Me"),
    ("Top Gun Danger Zone", "Danger Zone"),
    ("Top Gun Take My Breath Away", "Take My Breath"),
    ("Top Gun Hold My Hand", "Hold My Hand"),
    ("Chad Kroeger Hero", "Hero"),
    ("Barbie Dance The Night", "Dance The Night"),
    ("Survivor Eye Of The Tiger", "Eye Of The Tiger"),
    ("Ghostbusters Ray Parker", "Ghostbusters"),
    ("Wiz Khalifa See You Again", "See You Again"),
    ("Greatest Showman A Million Dreams", "Million Dreams"),
    ("Greatest Showman Never Enough", "Never Enough"),
    ("Stillwater Fever Dog", "Fever Dog"),

    # K-Drama
    ("DOTS This Love Davichi", "This Love"),
    ("DOTS Everytime Punch", "Everytime"),
    ("DOTS You Are My Everything Gummy", "You Are My Everything"),
    ("CLOY Sunset Davichi", "Sunset"),
    ("CLOY Give You My Heart IU", "Give You My Heart"),
    ("Lee Juck Don't Worry", "Don't Worry"),
    ("Taeyeon All About You", "All About You"),
    ("Hotel Del Luna Remember Me Gummy", "Remember Me"),

    # Indo
    ("Sherina Lihatlah Lebih Dekat", "Lihatlah"),
    ("Sherina Menikmati Hari", "Menikmati"),
    ("Sherina Bintang-Bintang", "Bintang"),
    ("Sherina Persahabatan", "Persahabatan"),
    ("Acha Septriasa Berdua Lebih Baik", "Berdua Lebih Baik"),
    ("Hanin Dhiya Kau Ada", "Kau Ada"),
    ("BCL Harta Berharga", "Harta Berharga"),
    ("Rano Karno Si Doel", "Si Doel"),
]

for q, expected in TRACKS:
    res = search_ug(q)
    matched = None
    for r in res:
        song = r.get('song_name', '')
        art = r.get('artist_name', '')
        if expected.lower() in song.lower() or expected.lower() in art.lower():
            matched = r
            break
    if matched:
        print(f"MATCH: '{q}' -> {matched.get('artist_name')} - {matched.get('song_name')} (votes {matched.get('votes')}): {matched.get('tab_url')}", flush=True)
    else:
        # print first result if any
        if res:
            print(f"CANDIDATE: '{q}' -> 1st result: {res[0].get('artist_name')} - {res[0].get('song_name')}: {res[0].get('tab_url')}", flush=True)
        else:
            print(f"NONE: '{q}'", flush=True)
    time.sleep(0.3)
