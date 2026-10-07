import urllib.request
import json
import html
import re

headers = {'User-Agent': 'Mozilla/5.0'}

for q in ['Rudderless Home', 'Rudderless Hold On', 'Billy Crudup']:
    url = f"https://www.ultimate-guitar.com/search.php?title={urllib.parse.quote(q)}&type=300"
    req = urllib.request.Request(url, headers=headers)
    try:
        content = urllib.request.urlopen(req).read().decode('utf-8')
        m = re.search(r'class="js-store" data-content="([^"]+)"', content)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
            print(f"Results for '{q}': {len(results)}")
            for r in results:
                print(f"  {r.get('artist_name')} - {r.get('song_name')} ({r.get('type')}): {r.get('tab_url')}")
    except Exception as e:
        print(f"Error {q}: {e}")
