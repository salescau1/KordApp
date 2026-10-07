import urllib.request
import json
import re
import html

url = 'https://tabs.ultimate-guitar.com/tab/adele/someone-like-you-chords-1006751'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9',
}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        content = resp.read().decode('utf-8', errors='ignore')
        m = re.search(r'class="js-store" data-content="([^"]+)"', content)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            tab_data = data.get('store', {}).get('page', {}).get('data', {}).get('tab_view', {})
            wiki_tab = tab_data.get('wiki_tab', {})
            print('SUCCESS LIVE SCRAPE UG ADELE:')
            print('  Song:', tab_data.get('song_name'))
            print('  Artist:', tab_data.get('artist_name'))
            print('  Key:', tab_data.get('meta', {}).get('tonality'))
            print('  Content length:', len(wiki_tab.get('content', '')))
            print('  First 200 chars:\n', repr(wiki_tab.get('content', '')[:200]))
        else:
            print('js-store not found')
except Exception as e:
    print('Error:', e)
