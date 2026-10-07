import urllib.request
import re
import json

url = 'https://guitartuna.com/chords/someone-like-you-adele-easy-guitar-chords-61094364addec83eff2e8055'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        content = resp.read().decode('utf-8', errors='ignore')
        print(f"SUCCESS LIVE SCRAPE GT ADELE! Length: {len(content)}")
        chords = set(re.findall(r'data-chord="([^"]+)"', content))
        print("Chords detected in HTML:", chords)
except Exception as e:
    print('Error GT:', e)
