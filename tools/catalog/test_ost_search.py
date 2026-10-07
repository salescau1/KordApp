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

TEST_QUERIES = [
    # Billy Crudup / Rudderless
    ("Rudderless Sing Along", "Billy Crudup", "Sing Along"),
    ("Rudderless Real Friends", "Billy Crudup", "Real Friends"),
    ("Rudderless Stay With You", "Billy Crudup", "Stay With You"),
    ("Rudderless Over Your Shoulder", "Billy Crudup", "Over Your Shoulder"),
    ("Rudderless Asshole", "Billy Crudup", "Asshole"),
    # Anime
    ("Hotaru no Hikari Ikimonogakari", "Ikimono-Gakari", "Hotaru no Hikari"),
    ("Lovers 7!!", "7!!", "Lovers"),
    ("Toumei Datta Sekai Motohiro Hata", "Motohiro Hata", "Toumei Datta Sekai"),
    ("Zenzenzense Radwimps", "RADWIMPS", "Zenzenzense"),
    ("Zankyosanka Aimer", "Aimer", "Zankyosanka"),
    ("Shinzou wo Sasageyo", "Linked Horizon", "Shinzou wo Sasageyo!"),
    ("Guren no Yumiya", "Linked Horizon", "Guren no Yumiya"),
    ("Akuma no Ko Ai Higuchi", "Ai Higuchi", "Akuma no Ko"),
    ("We Are Hiroshi Kitadani", "Hiroshi Kitadani", "We Are!"),
    ("Butter-Fly Koji Wada", "Koji Wada", "Butter-Fly"),
    ("Dan Dan Kokoro Hikareteku Field of View", "Field of View", "Dan Dan Kokoro Hikareteku"),
    ("Cha-La Head-Cha-La", "Hironobu Kageyama", "Cha-La Head-Cha-La"),
    ("Unravel TK", "TK from Ling Tosite Sigure", "Unravel"),
    ("Every Heart BoA", "BoA", "Every Heart"),
    ("1/3 no Junjou na Kanjou Siam Shade", "SIAM SHADE", "1/3 no Junjou na Kanjou"),
    ("Heart of Sword TM Revolution", "T.M.Revolution", "Heart of Sword"),
    ("Sobakasu Judy and Mary", "Judy and Mary", "Sobakasu"),
    ("Sekai ga Owaru Made wa WANDS", "WANDS", "Sekai ga Owaru Made wa"),
    ("Kimi ga Suki da to Sakebitai BAAD", "BAAD", "Kimi ga Suki da to Sakebitai"),
    # Disney & Western
    ("Let It Go Idina Menzel", "Idina Menzel", "Let It Go"),
    ("Into the Unknown Idina Menzel", "Idina Menzel", "Into the Unknown"),
    ("A Whole New World Peabo Bryson", "Peabo Bryson", "A Whole New World"),
    ("I See The Light Mandy Moore", "Mandy Moore", "I See The Light"),
    ("When Will My Life Begin Mandy Moore", "Mandy Moore", "When Will My Life Begin"),
    ("You've Got A Friend In Me Randy Newman", "Randy Newman", "You've Got A Friend In Me"),
    ("How Far I'll Go Aulii Cravalho", "Auli'i Cravalho", "How Far I'll Go"),
    ("We Don't Talk About Bruno", "Lin-Manuel Miranda", "We Don't Talk About Bruno"),
    ("Go The Distance Michael Bolton", "Michael Bolton", "Go The Distance"),
    ("Colors Of The Wind Vanessa Williams", "Vanessa Williams", "Colors Of The Wind"),
    ("Remember Me Anthony Gonzalez", "Anthony Gonzalez", "Remember Me"),
    ("You'll Be In My Heart Phil Collins", "Phil Collins", "You'll Be In My Heart"),
    ("Breaking Free High School Musical", "High School Musical Cast", "Breaking Free"),
    ("Danger Zone Kenny Loggins", "Kenny Loggins", "Danger Zone"),
    ("Take My Breath Away Berlin", "Berlin", "Take My Breath Away"),
    ("Hold My Hand Lady Gaga", "Lady Gaga", "Hold My Hand"),
    ("Hero Chad Kroeger", "Chad Kroeger", "Hero"),
    ("Dance The Night Dua Lipa", "Dua Lipa", "Dance The Night"),
    ("Eye of the Tiger Survivor", "Survivor", "Eye of the Tiger"),
    ("Ghostbusters Ray Parker Jr", "Ray Parker Jr.", "Ghostbusters"),
    ("See You Again Wiz Khalifa", "Wiz Khalifa", "See You Again"),
    # K-Drama
    ("This Love Davichi", "Davichi", "This Love"),
    ("Everytime Punch Chen", "Punch", "Everytime"),
    ("You Are My Everything Gummy", "Gummy", "You Are My Everything"),
    ("Sunset Davichi", "Davichi", "Sunset"),
    ("Don't Worry Lee Juck", "Lee Juck", "Don't Worry"),
    ("All About You Taeyeon", "Taeyeon", "All About You"),
    # Indo
    ("Lihatlah Lebih Dekat Sherina", "Sherina Munaf", "Lihatlah Lebih Dekat"),
    ("Menikmati Hari Sherina", "Sherina Munaf", "Menikmati Hari"),
    ("Bintang-Bintang Sherina", "Sherina Munaf", "Bintang-Bintang"),
    ("Persahabatan Sherina", "Sherina Munaf", "Persahabatan"),
    ("Berdua Lebih Baik Acha Septriasa", "Acha Septriasa", "Berdua Lebih Baik"),
    ("Kau Ada Hanin Dhiya", "Hanin Dhiya", "Kau Ada"),
    ("Harta Berharga Bunga Citra Lestari", "Bunga Citra Lestari", "Harta Berharga"),
    ("Si Doel Anak Sekolahan Rano Karno", "Rano Karno", "Si Doel Anak Sekolahan"),
    ("Selamat Jalan Kekasih Wizzy", "Wizzy", "Selamat Jalan Kekasih"),
]

def search_ug(query):
    url = f"https://www.ultimate-guitar.com/search.php?title={urllib.parse.quote(query)}&type=300"
    req = urllib.request.Request(url, headers=headers)
    try:
        content = urllib.request.urlopen(req, timeout=10).read().decode('utf-8')
        m = re.search(r'class="js-store"\s+data-content="([^"]+)"', content)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
            chord_results = [r for r in results if r.get('type') == 'Chords']
            if chord_results:
                # pick highest votes
                chord_results.sort(key=lambda x: x.get('votes', 0), reverse=True)
                top = chord_results[0]
                return top.get('song_name'), top.get('artist_name'), top.get('votes'), top.get('tab_url')
    except Exception as e:
        pass
    return None

success = 0
for q, art, song in TEST_QUERIES:
    res = search_ug(q)
    if res:
        print(f"OK: [{art} - {song}] -> UG: {res[1]} - {res[0]} (votes {res[2]}): {res[3]}")
        success += 1
    else:
        # Try simplified query
        res2 = search_ug(song)
        if res2:
            print(f"OK (fallback): [{art} - {song}] -> UG: {res2[1]} - {res2[0]} (votes {res2[2]}): {res2[3]}")
            success += 1
        else:
            print(f"MISSING: [{art} - {song}] (query '{q}')")
    time.sleep(0.2)

print(f"\nFound {success} / {len(TEST_QUERIES)} target songs!")
