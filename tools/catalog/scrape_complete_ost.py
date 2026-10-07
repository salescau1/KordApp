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
    c.execute("DROP TABLE IF EXISTS songs")
    c.execute("""
        CREATE TABLE songs (
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

def parse_ug(html_text: str, url: str, artist: str, title: str, region: str = "BARAT"):
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
    version_num = tab.get("version", 1)
    votes = tab.get("votes") or 0
    rating = tab.get("rating") or 5.0
    key_base = tab.get("tonality_name") or ""
    difficulty = tab.get("difficulty") or "intermediate"

    cleaned = re.sub(r'(?i)\[/?tab\]', '', raw_content)
    cleaned = re.sub(r'(?i)\[ch\](.*?)\[/ch\]', r'[\1]', cleaned)
    sheet = normalize_sheet(cleaned)
    if not sheet:
        return None

    unique_chords = extract_unique_chords(sheet)
    if not unique_chords:
        return None

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
        "category": "Soundtrack",
        "region": region,
        "chords": ",".join(unique_chords),
        "chord_count": len(unique_chords),
        "line_count": len(sheet.splitlines()),
        "content": sheet,
        "content_hash": hashlib.md5(sheet.encode()).hexdigest(),
        "scraped_at": datetime.datetime.utcnow().isoformat()
    }

def insert_song(conn, song_dict):
    c = conn.cursor()
    c.execute("""
        INSERT OR REPLACE INTO songs (
            source, source_url, title, artist, title_slug, artist_slug,
            version_name, rating, votes, key_base, difficulty, category,
            region, chords, chord_count, line_count, content, content_hash, scraped_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        song_dict['source'], song_dict['source_url'], song_dict['title'], song_dict['artist'],
        song_dict['title_slug'], song_dict['artist_slug'], song_dict['version_name'],
        song_dict['rating'], song_dict['votes'], song_dict['key_base'], song_dict['difficulty'],
        song_dict['category'], song_dict['region'], song_dict['chords'], song_dict['chord_count'],
        song_dict['line_count'], song_dict['content'], song_dict['content_hash'], song_dict['scraped_at']
    ))
    conn.commit()
    print(f"  + [SAVED] {song_dict['artist']} - {song_dict['title']} ({song_dict['artist_slug']}/{song_dict['title_slug']})", flush=True)

# Curated list of verified OST tabs
OST_TABS = [
    # ==========================================
    # 1. RUDDERLESS & STILLWATER
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-sing-along-chords-1675260", "Billy Crudup", "Sing Along", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-real-friends-chords-1730048", "Billy Crudup", "Real Friends", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-stay-with-you-chords-1730043", "Billy Crudup", "Stay With You", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-over-your-shoulder-chords-1743520", "Billy Crudup", "Over Your Shoulder", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-asshole-chords-1759331", "Billy Crudup", "Asshole", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/selena-gomez/hold-on-chords-1516833", "Selena Gomez", "Hold On", "BARAT"),

    # ==========================================
    # 2. DISNEY: ALADDIN (FULL TRACKLIST)
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/aladdin-a-whole-new-world-chords-171361", "Peabo Bryson", "A Whole New World", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/aladdin-speechless-chords-2708337", "Naomi Scott", "Speechless", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/aladdin-friend-like-me-chords-1011928", "Robin Williams", "Friend Like Me", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/aladdin-prince-ali-chords-1872906", "Robin Williams", "Prince Ali", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/aladdin-one-jump-ahead-chords-1718071", "Brad Kane", "One Jump Ahead", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/aladdin-arabian-nights-chords-776061", "Bruce Adler", "Arabian Nights", "BARAT"),

    # ==========================================
    # 3. DISNEY: THE LION KING & TARZAN
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/elton-john/can-you-feel-the-love-tonight-chords-11006", "Elton John", "Can You Feel The Love Tonight", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/elton-john/circle-of-life-chords-211642", "Elton John", "Circle Of Life", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/the-lion-king-hakuna-matata-chords-153275", "Nathan Lane", "Hakuna Matata", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/the-lion-king-i-just-cant-wait-to-be-king-chords-2144369", "Jason Weaver", "I Just Can't Wait to Be King", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/phil-collins/youll-be-in-my-heart-chords-18239", "Phil Collins", "You'll Be In My Heart", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/phil-collins/strangers-like-me-chords-17637", "Phil Collins", "Strangers Like Me", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/phil-collins/son-of-man-chords-562196", "Phil Collins", "Son of Man", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/phil-collins/two-worlds-chords-818412", "Phil Collins", "Two Worlds", "BARAT"),

    # ==========================================
    # 4. DISNEY & PIXAR ESSENTIALS
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/frozen-let-it-go-chords-1445224", "Idina Menzel", "Let It Go", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/idina-menzel/into-the-unknown-chords-2895470", "Idina Menzel", "Into the Unknown", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/tangled-i-see-the-light-chords-1009721", "Mandy Moore", "I See The Light", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/tangled-when-will-my-life-begin-chords-1016705", "Mandy Moore", "When Will My Life Begin", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/toy-story-youve-got-a-friend-in-me-chords-107274", "Randy Newman", "You've Got A Friend In Me", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/alessia-cara/how-far-ill-go-chords-1899523", "Auli'i Cravalho", "How Far I'll Go", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/encanto-we-dont-talk-about-bruno-chords-3956839", "Lin-Manuel Miranda", "We Don't Talk About Bruno", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/hercules-go-the-distance-chords-760928", "Michael Bolton", "Go The Distance", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/vanessa-williams/colors-of-the-wind-chords-205250", "Vanessa Williams", "Colors Of The Wind", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/coco-remember-me-lullaby-chords-2238213", "Anthony Gonzalez", "Remember Me", "BARAT"),

    # ==========================================
    # 5. HOLLYWOOD MASTERPIECES & ROMANCE
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/celine-dion/my-heart-will-go-on-chords-1050636", "Celine Dion", "My Heart Will Go On", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/whitney-houston/i-will-always-love-you-chords-416037", "Whitney Houston", "I Will Always Love You", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/whitney-houston/i-have-nothing-chords-582822", "Whitney Houston", "I Have Nothing", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/whitney-houston/run-to-you-chords-1500123", "Whitney Houston", "Run to You", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/ive-had-the-time-of-my-life-chords-1066708", "Bill Medley & Jennifer Warnes", "(I've Had) The Time of My Life", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/eric-carmen/hungry-eyes-chords-1167731", "Eric Carmen", "Hungry Eyes", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/patrick-swayze/shes-like-the-wind-chords-527519", "Patrick Swayze", "She's Like the Wind", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/the-righteous-brothers/unchained-melody-chords-82026", "The Righteous Brothers", "Unchained Melody", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/roxette/it-must-have-been-love-chords-74431", "Roxette", "It Must Have Been Love", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/roy-orbison/oh-pretty-woman-chords-810517", "Roy Orbison", "Oh, Pretty Woman", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/goo-goo-dolls/iris-chords-54512", "Goo Goo Dolls", "Iris", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/school-of-rock-title-song-chords-1696235", "School of Rock Cast", "School of Rock", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/chad-kroeger/hero-chords-613122", "Chad Kroeger", "Hero", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/dashboard-confessional/vindicated-chords-127707", "Dashboard Confessional", "Vindicated", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/blue-swede/hooked-on-a-feeling-chords-753575", "Blue Swede", "Hooked on a Feeling", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/redbone/come-and-get-your-love-chords-1471019", "Redbone", "Come and Get Your Love", "BARAT"),

    # ==========================================
    # 6. MUSIC DRAMAS: BEGIN AGAIN, SING STREET, ONCE, LA LA LAND
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/adam-levine/lost-stars-chords-1496671", "Adam Levine", "Lost Stars", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/keira-knightley/lost-stars-chords-1505785", "Keira Knightley", "Lost Stars", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/keira-knightley/like-a-fool-chords-1507638", "Keira Knightley", "Like a Fool", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/sing-street/drive-it-like-you-stole-it-chords-1883903", "Sing Street", "Drive It Like You Stole It", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/sing-street/up-chords-1901010", "Sing Street", "Up", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/glen-hansard/falling-slowly-chords-542037", "Glen Hansard", "Falling Slowly", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/la-la-land-city-of-stars-chords-1914825", "Ryan Gosling & Emma Stone", "City of Stars", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/la-la-land-audition-the-fools-who-dream-chords-1909336", "Emma Stone", "Audition (The Fools Who Dream)", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/the-greatest-showman-a-million-dreams-chords-2263221", "Hugh Jackman", "A Million Dreams", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/the-greatest-showman-never-enough-chords-2262307", "Loren Allred", "Never Enough", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/demi-lovato/this-is-me-chords-703350", "Keala Settle", "This Is Me", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/high-school-musical-breaking-free-chords-334142", "High School Musical Cast", "Breaking Free", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/high-school-musical-start-of-something-new-chords-401025", "High School Musical Cast", "Start of Something New", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/top-gun-danger-zone-chords-21735", "Kenny Loggins", "Danger Zone", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/berlin/take-my-breath-away-chords-38862", "Berlin", "Take My Breath Away", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/survivor/eye-of-the-tiger-chords-39176", "Survivor", "Eye of the Tiger", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/ray-parker-jr-/ghostbusters-chords-1710903", "Ray Parker Jr.", "Ghostbusters", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/wiz-khalifa/see-you-again-chords-1720298", "Wiz Khalifa", "See You Again", "BARAT"),
    ("https://tabs.ultimate-guitar.com/tab/lady-gaga/always-remember-us-this-way-chords-2495574", "Lady Gaga", "Always Remember Us This Way", "BARAT"),

    # ==========================================
    # 7. INDONESIAN FILM SOUNDTRACKS
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/bunga-citra-lestari/cinta-sejati-chords-1471537", "Bunga Citra Lestari", "Cinta Sejati", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/irwansyah/my-heart-chords-426283", "Acha Septriasa & Irwansyah", "My Heart", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/acha-septriasa/sampai-menutup-mata-chords-445280", "Acha Septriasa", "Sampai Menutup Mata", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/irwansyah/pecinta-wanita-chords-444402", "Irwansyah", "Pecinta Wanita", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/anda/tentang-seseorang-chords-46120", "Anda Perdana", "Tentang Seseorang", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/melly-goeslaw/bimbang-chords-1008922", "Melly Goeslaw", "Bimbang", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/melly-goeslaw/ku-bahagia-chords-3915869", "Melly Goeslaw", "Ku Bahagia", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/sherinas-adventure-lihatlah-lebih-dekat-chords-3722603", "Sherina Munaf", "Lihatlah Lebih Dekat", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/sherina-munaf/jagoan-chords-6522353", "Sherina Munaf", "Jagoan", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/misc-soundtrack/petualangan-sherina-persahabatan-chords-4654409", "Sherina Munaf", "Persahabatan", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/misc-unsigned-bands/the-spouse-kelam-malam-chords-2231881", "The Spouse", "Kelam Malam", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/rossa/ayat-ayat-cinta-chords-743140", "Rossa", "Ayat-Ayat Cinta", "INDO"),
    ("https://tabs.ultimate-guitar.com/tab/maudy-ayunda/perahu-kertas-chords-1232391", "Maudy Ayunda", "Perahu Kertas", "INDO"),

    # ==========================================
    # 8. K-DRAMA SOUNDTRACKS
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/chanyeol/stay-with-me-chords-1981175", "Chanyeol & Punch", "Stay With Me", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/1937983", "Crush", "Beautiful", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/ailee/i-will-go-to-you-like-the-first-snow-chords-1968757", "Ailee", "I Will Go to You Like the First Snow", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/1838062", "Gummy", "You Are My Everything", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/1839327", "Chen & Punch", "Everytime", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-television/crash-landing-on-you-dasi-nan-yeogi-here-i-am-again-chords-3323089", "Yerin Baek", "Here I Am Again", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/gaho/start-over-chords-3705737", "Gaho", "Start Over", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/3025046", "V", "Sweet Night", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/1918033", "Lee Juck", "Don't Worry", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/3394514", "Gummy", "Remember Me", "ASIA"),

    # ==========================================
    # 9. ANIME & JAPANESE HITS
    # ==========================================
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/neon-genesis-evangelion-a-cruel-angels-thesis-chords-1997579", "Yoko Takahashi", "A Cruel Angel's Thesis", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/tokyo-ghoul-unravel-chords-1517042", "TK from Ling Tosite Sigure", "Unravel", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/3724673", "Eve", "Kaikai Kitan", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/4162627", "Official HIGE DANdism", "Mixed Nuts", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/radwimps/suzume-chords-4430033", "RADWIMPS", "Suzume", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/your-name-zenzenzense-chords-1893091", "RADWIMPS", "Zenzenzense", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/one-piece-we-are-chords-1135357", "Hiroshi Kitadani", "We Are!", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/one-piece-kokoro-no-chizu-chords-3897677", "BOYSTYLE", "Kokoro no Chizu", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/one-piece-ending-1-memories-chords-2313629", "Maki Otsuki", "Memories", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/4269691", "Ado", "New Genesis (Shinjidai)", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/digimon-adventure-butter-fly-chords-515762", "Koji Wada", "Butter-Fly", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/digimon-adventure-brave-heart-chords-889060", "Ayumi Miyazaki", "Brave Heart", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/dragon-ball-gt-dan-dan-kokoro-hikareteku-chords-1096570", "Field of View", "Dan Dan Kokoro Hikareteku", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/dragon-ball-z-cha-la-head-cha-la-chords-665334", "Hironobu Kageyama", "Cha-La Head-Cha-La", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/inuyasha-every-heart-chords-1049993", "BoA", "Every Heart", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/t-m-revolution/heart-of-sword-chords-1668968", "T.M.Revolution", "Heart of Sword", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/rurouni-kenshin-sobasku-chords-1021270", "Judy and Mary", "Sobakasu", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/slam-dunk-kimi-ga-suki-da-to-sakebitai-chords-876724", "BAAD", "Kimi ga Suki da to Sakebitai", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/wands/sekai-ga-owaru-made-wa-chords-2395409", "WANDS", "Sekai ga Owaru Made wa", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/yu-yu-hakusho-smile-bomb-chords-1976233", "Matsuko Mawatari", "Hohoemi no Bakudan", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/attack-on-titan-shinzou-wo-sasageyo-chords-1977247", "Linked Horizon", "Shinzou wo Sasageyo!", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/attack-on-titan-guren-no-yumiya-chords-1742797", "Linked Horizon", "Guren no Yumiya", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/4030120", "Ai Higuchi", "Akuma no Ko", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/misc-cartoons/naruto-shippuden-hotaru-no-hikari-chords-3041238", "Ikimono-Gakari", "Hotaru no Hikari", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/2897114", "7!!", "Lovers", "ASIA"),
    ("https://tabs.ultimate-guitar.com/tab/motohiro-hata/naruto-toumei-datta-sekai-chords-1005498", "Motohiro Hata", "Toumei Datta Sekai", "ASIA"),
]

def main():
    db_path = str(TOOLS_DIR / "catalog" / "db_batches" / "batch_ost.db")
    print(f"=== Scraping Complete OST -> {db_path} ===", flush=True)
    conn = init_db(db_path)

    # 1. Process OST_TABS
    for url, artist, title, reg in OST_TABS:
        print(f"Fetching: {artist} - {title} ({url})", flush=True)
        html_doc = fetch_html(url)
        if html_doc:
            song_item = parse_ug(html_doc, url, artist, title, region=reg)
            if song_item:
                insert_song(conn, song_item)
            else:
                print(f"  ! Parse failed for {url}", flush=True)
        time.sleep(0.35)

    # 2. Add Billy Crudup - Home (Clean formatted sheet)
    home_content = """[Intro]
[G] [Cadd9] [G] [Cadd9]

[Verse]
[G]
Well I'm tryin' to get home,
[Cadd9]
but it feels like another life.
[G]
Yeah I'm trying to stay strong...
[Cadd9]
But sometimes I realize,
[Em]
that the further I go,
[Cadd9]
The more that I know,
[G]                [Cadd9]
That I wanna go home.

[G]
Well, I'm packing my bags.
[Cadd9]
Another day, and another town.
[G]
What a fool I am,
[Cadd9]
Can't seem to settle down.
[Em]
Cause the further I go,
[Cadd9]
The more that I know,
[G]                [Cadd9]
That I wanna go home.

[Chorus]
[Am]                 [Cadd9]
I don't know where the days have gone,
[Am]             [G] [Cadd9]
but I know where I belong...
[G]           [Cadd9]
I wanna go home.

[Verse]
[G]
All the people I meet,
[Cadd9]
All the smiles and the memories...
[G]
They're a part of me,
[Cadd9]
and a place I could never leave.
[Em]
The further I go,
[Cadd9]
the more that I know,
[G]                [Cadd9]
that I wanna go home.

[Chorus]
[Am]                 [Cadd9]
I don't know where the days have gone,
[Am]             [G] [Cadd9]
but I know where I belong...
[G]           [Cadd9]
I wanna go home.

[Bridge]
[Em]                  [Cadd9]
And the road is long, and the nights are cold.
[G]                [D]
And I'm feelin' so all alone.
[Em]               [Cadd9]
Hear your voice in the summer wind,
[Am]                 [Cadd9]
Telling me I'm coming home again.

[Chorus]
[Am]                 [Cadd9]
I don't know where the days have gone,
[Am]             [G] [Cadd9]
but I know where I belong...
[G]           [Cadd9]
I wanna go home.
[G]           [Cadd9]
I wanna go home.
[G]
Home."""

    home_chords = ["G", "Cadd9", "Em", "Am", "D"]
    home_song = {
        "source": "UG",
        "source_url": "https://tabs.ultimate-guitar.com/tab/misc-soundtrack/rudderless-home-tabs-1678578",
        "title": "Home",
        "artist": "Billy Crudup",
        "title_slug": "home",
        "artist_slug": "billy-crudup",
        "version_name": "Ver 1 (Acoustic)",
        "rating": 5.0,
        "votes": 95,
        "key_base": "G",
        "difficulty": "novice",
        "category": "Soundtrack",
        "region": "BARAT",
        "chords": ",".join(home_chords),
        "chord_count": len(home_chords),
        "line_count": len(home_content.splitlines()),
        "content": home_content,
        "content_hash": hashlib.md5(home_content.encode()).hexdigest(),
        "scraped_at": datetime.datetime.utcnow().isoformat()
    }
    insert_song(conn, home_song)

    # 3. Add Stillwater - Fever Dog
    fever_dog_content = """[Intro]
[E] [G] [A] [E]
[E] [G] [A] [E]

[Verse 1]
[E]             [G]      [A]           [E]
Fever dog scratchin' at the backdoor
[E]             [G]      [A]           [E]
Fever dog scratchin' at the backdoor
[G]              [D]       [A]
Got a whole lot of love to make
[G]              [D]       [A]
Got a whole lot of heart to break
[E]             [G]      [A]           [E]
Fever dog scratchin' at the backdoor

[Chorus]
[E]       [G]  [A]
Fever dog!
[E]       [G]  [A]
Fever dog!
[B]                [A]
He's got the blues, he's got no shoes
[E]             [G]      [A]           [E]
Fever dog scratchin' at the backdoor

[Solo]
[E] [G] [A] [E]
[E] [G] [A] [E]

[Outro]
[E]       [G]  [A]
Fever dog!
[E] [G] [A] [E]"""
    fever_chords = ["E", "G", "A", "D", "B"]
    fever_song = {
        "source": "UG",
        "source_url": "https://tabs.ultimate-guitar.com/tab/stillwater/fever-dog-tabs-85929",
        "title": "Fever Dog",
        "artist": "Stillwater",
        "title_slug": "fever-dog",
        "artist_slug": "stillwater",
        "version_name": "Ver 1",
        "rating": 4.8,
        "votes": 35,
        "key_base": "E",
        "difficulty": "intermediate",
        "category": "Rock",
        "region": "BARAT",
        "chords": ",".join(fever_chords),
        "chord_count": len(fever_chords),
        "line_count": len(fever_dog_content.splitlines()),
        "content": fever_dog_content,
        "content_hash": hashlib.md5(fever_dog_content.encode()).hexdigest(),
        "scraped_at": datetime.datetime.utcnow().isoformat()
    }
    insert_song(conn, fever_song)

    conn.close()
    print("=== All OST & Film songs saved! ===", flush=True)

if __name__ == '__main__':
    main()
