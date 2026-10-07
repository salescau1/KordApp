import urllib.request
import json
import re
import html
from bs4 import BeautifulSoup

headers = {'User-Agent': 'Mozilla/5.0'}

# 1. Ultimate Guitar
url_ug = 'https://www.ultimate-guitar.com/search.php?title=Elkasih&type=300'
req = urllib.request.Request(url_ug, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=10) as r:
        h = r.read().decode('utf-8', errors='replace')
        m = re.search(r'class="js-store" data-content="([^"]+)"', h)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
            print(f"UG results for Elkasih: {len(results)}")
            for x in results:
                print(f"  UG: {x.get('song_name')} | {x.get('tab_url')}")
except Exception as e:
    print('UG error:', e)

# 2. ChordTela
url_ct = 'https://www.chordtela.com/?s=elkasih'
req_ct = urllib.request.Request(url_ct, headers=headers)
try:
    with urllib.request.urlopen(req_ct, timeout=10) as r:
        h_ct = r.read().decode('utf-8', errors='replace')
        soup = BeautifulSoup(h_ct, 'html.parser')
        links = soup.find_all('a', href=re.compile(r'chordtela\.com/\d{4}/\d{2}/chord-'))
        print(f"ChordTela results for Elkasih: {len(links)}")
        seen = set()
        for a in links:
            href = a.get('href')
            text = a.get_text(strip=True)
            if href not in seen and 'elkasih' in (text + href).lower():
                seen.add(href)
                print(f"  CT: {text} | {href}")
except Exception as e:
    print('ChordTela error:', e)
