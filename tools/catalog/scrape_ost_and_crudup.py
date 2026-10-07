import datetime
import hashlib
import html
import json
import re
import sqlite3
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.models import ParsedSong
from crawler.normalizer import normalize_sheet, extract_unique_chords

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

def to_slug(text: str) -> str:
    s = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode('utf-8')
    s = re.sub(r'[^a-zA-Z0-9]+', '-', s).strip('-').lower()
    return s

def fetch_html(url: str) -> str:
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=12) as resp:
            return resp.read().decode('utf-8', errors='replace')
    except Exception as e:
        print(f"  [FETCH ERR] {url}: {e}", flush=True)
        return ""

def init_db(db_path: str):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE IF NOT EXISTS songs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source TEXT NOT NULL,
            source_url TEXT NOT NULL UNIQUE,
            title TEXT NOT NULL,
            artist TEXT NOT NULL,
            title_slug TEXT NOT NULL,
            artist_slug TEXT NOT NULL,
            version_name TEXT,
            rating REAL,
            votes INTEGER,
            key_base TEXT,
            difficulty TEXT,
            category TEXT,
            region TEXT,
            chords TEXT,
            chord_count INTEGER,
            line_count INTEGER,
            content TEXT NOT NULL,
            content_hash TEXT,
            scraped_at TEXT
        )
    """)
    conn.commit()
    return conn

def parse_ug_page(html_text: str, url: str, override_artist: str = "", override_title: str = ""):
    m = re.search(r'class="js-store"\s+data-content="([^"]+)"', html_text)
    if not m:
        return None
    try:
        raw_json = html.unescape(m.group(1))
        data = json.loads(raw_json)
    except Exception:
        return None

    page_data = data.get("store", {}).get("page", {}).get("data", {})
    tab_view = page_data.get("tab_view", {})
    raw_content = tab_view.get("wiki_tab", {}).get("content", "")
    if not raw_content:
        return None

    tab = page_data.get("tab", {})
    orig_title = tab.get("song_name", "")
    orig_artist = tab.get("artist_name", "")

    title = override_title or orig_title
    artist = override_artist or orig_artist

    # Clean title if it contains "Rudderless - "
    if "rudderless - " in title.lower():
        title = re.sub(r'(?i)rudderless\s*-\s*', '', title).strip()

    cleaned = re.sub(r'(?i)\[/?tab\]', '', raw_content)
    cleaned = re.sub(r'(?i)\[ch\](.*?)\[/ch\]', r'[\1]', cleaned)
    sheet = normalize_sheet(cleaned)
    if not sheet:
        return None

    unique_chords = extract_unique_chords(sheet)
    if not unique_chords:
        return None

    version_num = tab.get("version", 1)
    votes = tab.get("votes") or 0
    rating = tab.get("rating") or 5.0
    key_base = tab.get("tonality_name") or ""
    difficulty = tab.get("difficulty") or "intermediate"

    return {
        "source": "UG",
        "source_url": url,
        "title": title,
        "artist": artist,
        "title_slug": to_slug(title),
        "artist_slug": to_slug(artist),
        "version_name": f"Ver {version_num}",
        "rating": float(rating),
        "votes": int(votes),
        "key_base": key_base,
        "difficulty": difficulty,
        "category": "Pop",
        "region": "BARAT",
        "chords": ",".join(unique_chords),
        "chord_count": len(unique_chords),
        "line_count": len(sheet.splitlines()),
        "content": sheet,
        "content_hash": hashlib.md5(sheet.encode()).hexdigest(),
        "scraped_at": datetime.datetime.utcnow().isoformat()
    }

def search_ug_chords(query: str):
    url = f"https://www.ultimate-guitar.com/search.php?title={urllib.parse.quote(query)}&type=300"
    html_text = fetch_html(url)
    m = re.search(r'class="js-store"\s+data-content="([^"]+)"', html_text)
    if not m:
        return []
    try:
        data = json.loads(html.unescape(m.group(1)))
        results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
        chord_tabs = [r for r in results if r.get('type') == 'Chords']
        chord_tabs.sort(key=lambda x: x.get('votes', 0), reverse=True)
        return chord_tabs
    except Exception:
        return []

def main():
    db_path = str(TOOLS_DIR / "catalog" / "db_batches" / "batch_ost.db")
    print(f"=== Starting OST & Billy Crudup Scraper -> {db_path} ===", flush=True)
    conn = init_db(db_path)

    # 1. DIRECT URLS for known specific soundtracks (Billy Crudup & Rudderless, Stillwater, etc.)
    DIRECT_TABS = [
        # Billy Crudup / Rudderless
        ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-sing-along-chords-1675260", "Billy Crudup", "Sing Along", "BARAT"),
        ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-real-friends-chords-1730048", "Billy Crudup", "Real Friends", "BARAT"),
        ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-stay-with-you-chords-1730043", "Billy Crudup", "Stay With You", "BARAT"),
        ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-over-your-shoulder-chords-1743520", "Billy Crudup", "Over Your Shoulder", "BARAT"),
        ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-asshole-chords-1759331", "Billy Crudup", "Asshole", "BARAT"),
        ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-home-tabs-1678578", "Billy Crudup", "Home", "BARAT"),
    ]

    for tab_url, art, tit, reg in DIRECT_TABS:
        c = conn.cursor()
        if c.execute("SELECT id FROM songs WHERE source_url = ?", (tab_url,)).fetchone():
            continue
        print(f"Scraping Direct Tab: {art} - {tit} ({tab_url})", flush=True)
        html_doc = fetch_html(tab_url)
        if html_doc:
            item = parse_ug_page(html_doc, tab_url, override_artist=art, override_title=tit)
            if item:
                item['region'] = reg
                c.execute("""
                    INSERT INTO songs (
                        source, source_url, title, artist, title_slug, artist_slug,
                        version_name, rating, votes, key_base, difficulty, category,
                        region, chords, chord_count, line_count, content, content_hash, scraped_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    item['source'], item['source_url'], item['title'], item['artist'],
                    item['title_slug'], item['artist_slug'], item['version_name'],
                    item['rating'], item['votes'], item['key_base'], item['difficulty'],
                    item['category'], item['region'], item['chords'], item['chord_count'],
                    item['line_count'], item['content'], item['content_hash'], item['scraped_at']
                ))
                conn.commit()
                print(f"  + [SAVED] {item['artist']} - {item['title']} ({item['chords']})", flush=True)
        time.sleep(0.5)

    # 2. TARGET SEARCH FOR MISSING SOUNDTRACKS
    TARGET_SEARCHES = [
        # Anime
        ("Hotaru no Hikari Ikimonogakari", "Ikimono-Gakari", "Hotaru no Hikari", "ASIA"),
        ("Lovers 7 oops", "7!!", "Lovers", "ASIA"),
        ("Toumei Datta Sekai Motohiro Hata", "Motohiro Hata", "Toumei Datta Sekai", "ASIA"),
        ("Zenzenzense Radwimps", "RADWIMPS", "Zenzenzense", "ASIA"),
        ("Dream Lantern Radwimps", "RADWIMPS", "Dream Lantern (Yumetourou)", "ASIA"),
        ("Zankyosanka Aimer", "Aimer", "Zankyosanka", "ASIA"),
        ("Shinzou wo Sasageyo Linked Horizon", "Linked Horizon", "Shinzou wo Sasageyo!", "ASIA"),
        ("Guren no Yumiya Linked Horizon", "Linked Horizon", "Guren no Yumiya", "ASIA"),
        ("Akuma no Ko Ai Higuchi", "Ai Higuchi", "Akuma no Ko", "ASIA"),
        ("We Are Hiroshi Kitadani", "Hiroshi Kitadani", "We Are!", "ASIA"),
        ("Kokoro no Chizu Boystyle", "BOYSTYLE", "Kokoro no Chizu", "ASIA"),
        ("Memories Maki Otsuki", "Maki Otsuki", "Memories", "ASIA"),
        ("New Genesis Ado", "Ado", "New Genesis (Shinjidai)", "ASIA"),
        ("Butter-Fly Koji Wada", "Koji Wada", "Butter-Fly", "ASIA"),
        ("Brave Heart Ayumi Miyazaki", "Ayumi Miyazaki", "Brave Heart", "ASIA"),
        ("Dan Dan Kokoro Hikareteku Field of View", "Field of View", "Dan Dan Kokoro Hikareteku", "ASIA"),
        ("Cha-La Head-Cha-La Hironobu Kageyama", "Hironobu Kageyama", "Cha-La Head-Cha-La", "ASIA"),
        ("Unravel TK from Ling Tosite Sigure", "TK from Ling Tosite Sigure", "Unravel", "ASIA"),
        ("Every Heart BoA", "BoA", "Every Heart", "ASIA"),
        ("Doraemon no Uta Kumiko Osugi", "Kumiko Osugi", "Doraemon no Uta", "ASIA"),
        ("1/3 no Junjou na Kanjou Siam Shade", "SIAM SHADE", "1/3 no Junjou na Kanjou", "ASIA"),
        ("Heart of Sword TM Revolution", "T.M.Revolution", "Heart of Sword", "ASIA"),
        ("Sobakasu Judy and Mary", "Judy and Mary", "Sobakasu", "ASIA"),
        ("Sekai ga Owaru Made wa WANDS", "WANDS", "Sekai ga Owaru Made wa", "ASIA"),
        ("Kimi ga Suki da to Sakebitai BAAD", "BAAD", "Kimi ga Suki da to Sakebitai", "ASIA"),
        ("My Friend ZARD", "ZARD", "My Friend", "ASIA"),
        ("Unmei no Roulette Mawashite ZARD", "ZARD", "Unmei no Roulette Mawashite", "ASIA"),
        ("Secret of My Heart Mai Kuraki", "Mai Kuraki", "Secret of My Heart", "ASIA"),
        ("Hohoemi no Bakudan Matsuko Mawatari", "Matsuko Mawatari", "Hohoemi no Bakudan", "ASIA"),

        # Disney & Western
        ("Let It Go Idina Menzel", "Idina Menzel", "Let It Go", "BARAT"),
        ("Into the Unknown Idina Menzel", "Idina Menzel", "Into the Unknown", "BARAT"),
        ("A Whole New World Peabo Bryson", "Peabo Bryson", "A Whole New World", "BARAT"),
        ("I See The Light Mandy Moore", "Mandy Moore", "I See The Light", "BARAT"),
        ("When Will My Life Begin Mandy Moore", "Mandy Moore", "When Will My Life Begin", "BARAT"),
        ("You've Got A Friend In Me Randy Newman", "Randy Newman", "You've Got A Friend In Me", "BARAT"),
        ("How Far I'll Go Aulii Cravalho", "Auli'i Cravalho", "How Far I'll Go", "BARAT"),
        ("We Don't Talk About Bruno Lin-Manuel Miranda", "Lin-Manuel Miranda", "We Don't Talk About Bruno", "BARAT"),
        ("Go The Distance Michael Bolton", "Michael Bolton", "Go The Distance", "BARAT"),
        ("Colors Of The Wind Vanessa Williams", "Vanessa Williams", "Colors Of The Wind", "BARAT"),
        ("Remember Me Anthony Gonzalez", "Anthony Gonzalez", "Remember Me", "BARAT"),
        ("You'll Be In My Heart Phil Collins", "Phil Collins", "You'll Be In My Heart", "BARAT"),
        ("Strangers Like Me Phil Collins", "Phil Collins", "Strangers Like Me", "BARAT"),
        ("Breaking Free High School Musical", "High School Musical Cast", "Breaking Free", "BARAT"),
        ("Start of Something New High School Musical", "High School Musical Cast", "Start of Something New", "BARAT"),
        ("Gotta Go My Own Way High School Musical", "High School Musical Cast", "Gotta Go My Own Way", "BARAT"),
        ("This Is Me Demi Lovato", "Demi Lovato", "This Is Me", "BARAT"),
        ("Gotta Find You Joe Jonas", "Joe Jonas", "Gotta Find You", "BARAT"),
        ("Danger Zone Kenny Loggins", "Kenny Loggins", "Danger Zone", "BARAT"),
        ("Take My Breath Away Berlin", "Berlin", "Take My Breath Away", "BARAT"),
        ("Hold My Hand Lady Gaga", "Lady Gaga", "Hold My Hand", "BARAT"),
        ("Always Remember Us This Way Lady Gaga", "Lady Gaga", "Always Remember Us This Way", "BARAT"),
        ("Hero Chad Kroeger", "Chad Kroeger", "Hero", "BARAT"),
        ("Dance The Night Dua Lipa", "Dua Lipa", "Dance The Night", "BARAT"),
        ("Eye of the Tiger Survivor", "Survivor", "Eye of the Tiger", "BARAT"),
        ("Ghostbusters Ray Parker Jr", "Ray Parker Jr.", "Ghostbusters", "BARAT"),
        ("See You Again Wiz Khalifa", "Wiz Khalifa", "See You Again", "BARAT"),
        ("A Million Dreams Hugh Jackman", "Hugh Jackman", "A Million Dreams", "BARAT"),
        ("Never Enough Loren Allred", "Loren Allred", "Never Enough", "BARAT"),
        ("Fever Dog Stillwater", "Stillwater", "Fever Dog", "BARAT"),

        # K-Drama
        ("This Love Davichi", "Davichi", "This Love", "ASIA"),
        ("Everytime Punch Chen", "Punch", "Everytime", "ASIA"),
        ("You Are My Everything Gummy", "Gummy", "You Are My Everything", "ASIA"),
        ("Sunset Davichi", "Davichi", "Sunset", "ASIA"),
        ("Give You My Heart IU", "IU", "Give You My Heart", "ASIA"),
        ("Don't Worry Lee Juck", "Lee Juck", "Don't Worry", "ASIA"),
        ("All About You Taeyeon", "Taeyeon", "All About You", "ASIA"),
        ("Remember Me Gummy", "Gummy", "Remember Me", "ASIA"),

        # Indo OST
        ("Lihatlah Lebih Dekat Sherina", "Sherina Munaf", "Lihatlah Lebih Dekat", "INDO"),
        ("Menikmati Hari Sherina", "Sherina Munaf", "Menikmati Hari", "INDO"),
        ("Bintang-Bintang Sherina", "Sherina Munaf", "Bintang-Bintang", "INDO"),
        ("Persahabatan Sherina", "Sherina Munaf", "Persahabatan", "INDO"),
        ("Mengenang Bintang Sherina", "Sherina Munaf", "Mengenang Bintang", "INDO"),
        ("Berdua Lebih Baik Acha Septriasa", "Acha Septriasa", "Berdua Lebih Baik", "INDO"),
        ("Ayat-Ayat Cinta 2 Krisdayanti", "Krisdayanti", "Ayat-Ayat Cinta 2", "INDO"),
        ("Kau Ada Hanin Dhiya", "Hanin Dhiya", "Kau Ada", "INDO"),
        ("Harta Berharga Bunga Citra Lestari", "Bunga Citra Lestari", "Harta Berharga", "INDO"),
        ("Si Doel Anak Sekolahan Rano Karno", "Rano Karno", "Si Doel Anak Sekolahan", "INDO"),
        ("Selamat Jalan Kekasih Wizzy", "Wizzy", "Selamat Jalan Kekasih", "INDO"),
    ]

    for q, art, song, reg in TARGET_SEARCHES:
        c = conn.cursor()
        row = c.execute("SELECT id FROM songs WHERE artist_slug = ? AND title_slug = ?", (to_slug(art), to_slug(song))).fetchone()
        if row:
            print(f"Already in DB: {art} - {song}", flush=True)
            continue

        print(f"Searching: '{q}' -> [{art} - {song}]", flush=True)
        results = search_ug_chords(q)
        if not results:
            # Fallback search by song title only
            results = search_ug_chords(song)

        if not results:
            print(f"  [NOT FOUND on UG]: {art} - {song}", flush=True)
            continue

        # Get the top voted chord version
        top_tab = results[0]
        tab_url = top_tab.get('tab_url')
        print(f"  -> Found top tab (votes {top_tab.get('votes')}): {tab_url}", flush=True)

        html_doc = fetch_html(tab_url)
        if html_doc:
            item = parse_ug_page(html_doc, tab_url, override_artist=art, override_title=song)
            if item:
                item['region'] = reg
                c.execute("""
                    INSERT INTO songs (
                        source, source_url, title, artist, title_slug, artist_slug,
                        version_name, rating, votes, key_base, difficulty, category,
                        region, chords, chord_count, line_count, content, content_hash, scraped_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    item['source'], item['source_url'], item['title'], item['artist'],
                    item['title_slug'], item['artist_slug'], item['version_name'],
                    item['rating'], item['votes'], item['key_base'], item['difficulty'],
                    item['category'], item['region'], item['chords'], item['chord_count'],
                    item['line_count'], item['content'], item['content_hash'], item['scraped_at']
                ))
                conn.commit()
                print(f"  + [SAVED] {item['artist']} - {item['title']} ({item['chords']})", flush=True)
            else:
                print(f"  ! [PARSE FAIL] {tab_url}", flush=True)
        time.sleep(0.5)

    conn.close()
    print("=== Finished scraping targeted OST! ===", flush=True)

if __name__ == '__main__':
    main()
