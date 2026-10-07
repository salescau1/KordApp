"""
Live Scraper for Adele & Queen songs directly from UG and GT to build a fresh catalog.db.
"""
import urllib.request
import re
import json
import html
import hashlib
import sqlite3
import os
import lzma
from datetime import datetime

OUT_DB = r"C:\KordApp-AGY\tools\catalog\fresh_test_catalog.db"
ASSET_XZ = r"C:\KordApp-AGY\app\src\main\assets\catalog.db.xz"

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9',
}

TARGET_URLS = [
    # UG
    {"source": "UG", "artist": "Adele", "title": "Someone Like You", "artist_slug": "adele", "title_slug": "someone-like-you", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/adele/someone-like-you-chords-1006751"},
    {"source": "UG", "artist": "Adele", "title": "Make You Feel My Love", "artist_slug": "adele", "title_slug": "make-you-feel-my-love", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/adele/make-you-feel-my-love-chords-752102"},
    {"source": "UG", "artist": "Queen", "title": "Bohemian Rhapsody", "artist_slug": "queen", "title_slug": "bohemian-rhapsody", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/queen/bohemian-rhapsody-chords-18733"},
    {"source": "UG", "artist": "Queen", "title": "Love of My Life", "artist_slug": "queen", "title_slug": "love-of-my-life", "version": "Ver 1", "url": "https://tabs.ultimate-guitar.com/tab/queen/love-of-my-life-chords-67439"},
    # GT
    {"source": "GT", "artist": "Adele", "title": "Make You Feel My Love", "artist_slug": "adele", "title_slug": "make-you-feel-my-love", "version": "Easy Chords", "url": "https://guitartuna.com/chords/make-you-feel-my-love-adele-easy-guitar-chords-6109442baddec83eff2e8189"},
    {"source": "GT", "artist": "Adele", "title": "When We Were Young", "artist_slug": "adele", "title_slug": "when-we-were-young", "version": "Easy Chords", "url": "https://guitartuna.com/chords/when-we-were-young-adele-easy-guitar-chords-61097afc1beff32aaef763ea"},
    {"source": "GT", "artist": "Queen", "title": "Bohemian Rhapsody", "artist_slug": "queen", "title_slug": "bohemian-rhapsody", "version": "Easy Chords", "url": "https://guitartuna.com/chords/bohemian-rhapsody-queen-easy-guitar-chords-61096a601beff32aaef74720"},
    {"source": "GT", "artist": "Queen", "title": "Love of My Life", "artist_slug": "queen", "title_slug": "love-of-my-life", "version": "Easy Chords", "url": "https://guitartuna.com/chords/love-of-my-life-queen-easy-guitar-chords-61096a841beff32aaef74744"},
]

def fetch_url(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return resp.read().decode('utf-8', errors='ignore')

def parse_ug(html_text):
    m = re.search(r'class="js-store" data-content="([^"]+)"', html_text)
    if not m:
        return None
    data = json.loads(html.unescape(m.group(1)))
    page_data = data.get('store', {}).get('page', {}).get('data', {})
    tab_view = page_data.get('tab_view', {})
    tab_info = page_data.get('tab', {})
    content = tab_view.get('wiki_tab', {}).get('content', '')
    
    # replace [ch]...[/ch] with [...]
    content = re.sub(r'\[ch\](.*?)\[/ch\]', r'[\1]', content)
    
    # extract chords
    chords = sorted(list(set(re.findall(r'\[([^\]]+)\]', content))))
    key_base = tab_view.get('meta', {}).get('tonality', '')
    rating = tab_info.get('rating', 0.0)
    votes = tab_info.get('votes', 0)
    difficulty = tab_view.get('meta', {}).get('difficulty', '')
    
    return {
        'content': content,
        'chords': json.dumps(chords),
        'chord_count': len(chords),
        'line_count': len(content.splitlines()),
        'key_base': key_base,
        'rating': rating,
        'votes': votes,
        'difficulty': difficulty,
    }

def parse_gt(html_text):
    # Parse Next.js RSC payload / HTML structure for GuitarTuna
    # Find chord spans: <span data-chord="C">C</span> or RSC payload
    # Let's check sections or lines
    lines = []
    # If React Server Component / Next payload:
    # A simple clean extractor for GuitarTuna:
    # Usually contains lines with data-chord or parts
    # Let's inspect how ChordApp scraper does it:
    chords_found = set(re.findall(r'data-chord="([^"]+)"', html_text))
    
    # Also find lyric lines if present
    # In Next.js, lines are often inside "className":"line" or standard HTML
    # We can use the existing gt_scraper parser logic
    from scraper import gt_scraper
    parsed = gt_scraper.parse_sheet(html_text)
    if parsed and parsed.get('content'):
        content = parsed['content']
        chords = sorted(list(set(re.findall(r'\[([^\]]+)\]', content))))
        return {
            'content': content,
            'chords': json.dumps(chords),
            'chord_count': len(chords),
            'line_count': len(content.splitlines()),
            'key_base': parsed.get('key_base', ''),
            'rating': 5.0,
            'votes': 100,
            'difficulty': 'Beginner',
        }
    return None
