import sys
sys.path.insert(0, r'C:\KordApp-AGY\tools')
from crawler.fetcher import fetch_html
import re, json, html

url = 'https://www.ultimate-guitar.com/search.php?title=Queen&type=300&page=2'
html_text = fetch_html(url)
m = re.search(r'class="js-store" data-content="([^"]+)"', html_text)
if m:
    data = json.loads(html.unescape(m.group(1)))
    page_data = data.get('store', {}).get('page', {}).get('data', {})
    results = page_data.get('results', [])
    pagination = page_data.get('pagination', {})
    total = page_data.get('total', 0)
    print('Page 2 results count:', len(results))
    print('Pagination info:', pagination)
    print('Total results:', total)
