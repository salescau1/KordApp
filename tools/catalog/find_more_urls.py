import html
import json
import re
import urllib.parse
import urllib.request

headers = {'User-Agent': 'Mozilla/5.0'}

def find_top_chord(q):
    url = f"https://www.ultimate-guitar.com/search.php?title={urllib.parse.quote(q)}&type=300"
    req = urllib.request.Request(url, headers=headers)
    try:
        content = urllib.request.urlopen(req, timeout=10).read().decode('utf-8')
        m = re.search(r'class="js-store"\s+data-content="([^"]+)"', content)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
            chords = [r for r in results if r.get('type') == 'Chords']
            if chords:
                chords.sort(key=lambda x: x.get('votes', 0), reverse=True)
                top = chords[0]
                print(f"'{q}' -> {top.get('artist_name')} - {top.get('song_name')} (votes {top.get('votes')}): {top.get('tab_url')}")
                return
    except Exception as e:
        print(f"ERR {q}: {e}")
    print(f"NOT FOUND: '{q}'")

find_top_chord("Danger Zone Kenny Loggins")
find_top_chord("Danger Zone Top Gun")
find_top_chord("Dance The Night Dua Lipa")
find_top_chord("Hold My Hand Lady Gaga")
find_top_chord("Always Remember Us This Way")
find_top_chord("You'll Be In My Heart Phil Collins")
find_top_chord("Into The Unknown Frozen")
find_top_chord("This Is Me Camp Rock")
find_top_chord("Aimer Zankyosanka")
find_top_chord("Secret of My Heart Mai Kuraki")
find_top_chord("1/3 no Junjou na Kanjou")
