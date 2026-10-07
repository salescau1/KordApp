import urllib.request
import json
import html
import re

headers = {'User-Agent': 'Mozilla/5.0'}
url = 'https://www.ultimate-guitar.com/search.php?title=Fever%20Dog'
req = urllib.request.Request(url, headers=headers)
content = urllib.request.urlopen(req).read().decode('utf-8')
m = re.search(r'class="js-store" data-content="([^"]+)"', content)
if m:
    data = json.loads(html.unescape(m.group(1)))
    results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
    for r in results:
        print(f"{r.get('artist_name')} - {r.get('song_name')} ({r.get('type')}, votes {r.get('votes')}): {r.get('tab_url')}")





