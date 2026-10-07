import datetime
import hashlib
import html
import json
import os
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

TOOLS_DIR = Path(r"C:\KordApp-AGY\tools")
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

def parse_ug(html_text: str, url: str, artist: str, title: str, region: str = "BARAT", category: str = "Soundtrack"):
    m = re.search(r'class="js-store"\s+data-content="([^"]+)"', html_text)
    if not m:
        return None
    try:
        raw_json = html.unescape(m.group(1))
        data = json.loads(raw_json)
        tab_view = data.get('store', {}).get('page', {}).get('data', {}).get('tab_view', {})
        tab = data.get('store', {}).get('page', {}).get('data', {}).get('tab', {})
        
        content = tab_view.get('wiki_tab', {}).get('content', '')
        if not content:
            return None
        
        # Clean [ch]...[/ch] tags to [Chord]
        cleaned_content = re.sub(r'\[ch\]([^\[]+?)\[/ch\]', r'[\1]', content)
        cleaned_content = re.sub(r'\[/?tab\]', '', cleaned_content)
        sheet_content = normalize_sheet(cleaned_content)
        unique_chords = extract_unique_chords(sheet_content)
        
        votes = tab.get('votes', 0)
        rating = tab.get('rating', 0.0)
        meta = tab_view.get('meta')
        if not isinstance(meta, dict):
            meta = {}
        difficulty = meta.get('difficulty', 'Intermediate') or 'Intermediate'
        key_base = meta.get('tonality', '') or ''
        version_name = f"Ver {tab.get('version', 1)}"
        
        title_slug = to_slug(title)
        artist_slug = to_slug(artist)
        content_hash = hashlib.md5(sheet_content.encode('utf-8')).hexdigest()
        scraped_at = datetime.datetime.now().isoformat()
        line_count = len(sheet_content.splitlines())
        chords_str = " ".join(unique_chords)
        
        return (
            "UG", url, title, artist, title_slug, artist_slug,
            version_name, rating, votes, key_base, difficulty, category,
            region, chords_str, len(unique_chords), line_count,
            sheet_content, content_hash, scraped_at
        )
    except Exception as e:
        print(f"  [PARSE ERR] {url}: {e}", flush=True)
        return None

# Curation list for Batch 23
UG_TARGETS = [
    # 1. Studio Ghibli
    ("Spirited Away - Always with Me", "https://tabs.ultimate-guitar.com/tab/928893", "Youmi Kimura", "Always with Me (Itsumo Nando Demo)", "JAPAN", "Soundtrack"),
    ("Howl's Moving Castle - Sekai no Yakusoku", "https://tabs.ultimate-guitar.com/tab/1678622", "Chieko Baisho & Joe Hisaishi", "The Promise of the World (Sekai no Yakusoku)", "JAPAN", "Soundtrack"),
    ("My Neighbor Totoro - Tonari no Totoro", "https://tabs.ultimate-guitar.com/tab/joe-hisaishi/tonari-no-totoro-chords-914271", "Azumi Inoue & Joe Hisaishi", "Tonari no Totoro", "JAPAN", "Soundtrack"),
    ("Kiki's Delivery Service - Rouge no Dengon", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/kikis-delivery-service-rouge-no-dengon-chords-939182", "Yumi Matsutoya", "Rouge no Dengon", "JAPAN", "Soundtrack"),
    ("Princess Mononoke - Theme", "https://tabs.ultimate-guitar.com/tab/joe-hisaishi/princess-mononoke-theme-song-mononoke-hime-chords-4688081", "Joe Hisaishi", "Mononoke Hime", "JAPAN", "Soundtrack"),

    # 2. Anime Anthems & Shonen
    ("Oshi no Ko - Idol", "https://tabs.ultimate-guitar.com/tab/yoasobi/idol-chords-4747505", "YOASOBI", "Idol", "JAPAN", "Soundtrack"),
    ("Jujutsu Kaisen - Ao no Sumika", "https://tabs.ultimate-guitar.com/tab/4947517", "Tatsuya Kitani", "Ao no Sumika (Where Our Blue Is)", "JAPAN", "Soundtrack"),
    ("Hunter x Hunter - Departure!", "https://tabs.ultimate-guitar.com/tab/masatoshi-ono/hunter-x-hunter-departure-chords-2900450", "Masatoshi Ono", "Departure!", "JAPAN", "Soundtrack"),
    ("Hunter x Hunter - Hyori Ittai", "https://tabs.ultimate-guitar.com/tab/1517104", "Yuzu", "Hyori Ittai", "JAPAN", "Soundtrack"),
    ("Hunter x Hunter - Hunting for Your Dream", "https://tabs.ultimate-guitar.com/tab/galneryus/hunting-for-your-dream-chords-2366101", "Galneryus", "Hunting for Your Dream", "JAPAN", "Soundtrack"),
    ("Your Lie in April - Hikaru Nara", "https://tabs.ultimate-guitar.com/tab/goose-house/hikaru-nara-chords-1679733", "Goose house", "Hikaru Nara", "JAPAN", "Soundtrack"),
    ("Your Lie in April - Orange", "https://tabs.ultimate-guitar.com/tab/1718463", "7!!", "Orange", "JAPAN", "Soundtrack"),
    ("Tokyo Revengers - Cry Baby", "https://tabs.ultimate-guitar.com/tab/official-hige-dandism/cry-baby-chords-3725384", "Official HIGE DANdism", "Cry Baby", "JAPAN", "Soundtrack"),
    ("Bocchi the Rock - Seishun Complex", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/bocchi-the-rock-seisyun-complex-chords-5864864", "Kessoku Band", "Seishun Complex", "JAPAN", "Soundtrack"),
    ("Bocchi the Rock - Ano Band", "https://tabs.ultimate-guitar.com/tab/4658051", "Kessoku Band", "Ano Band", "JAPAN", "Soundtrack"),
    ("Bocchi the Rock - Guitar Loneliness", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/bocchi-the-rock-guitar-loneliness-and-blue-planet-chords-4638332", "Kessoku Band", "Guitar, Loneliness and Blue Planet", "JAPAN", "Soundtrack"),
    ("Bleach - D-tecnolife", "https://tabs.ultimate-guitar.com/tab/uverworld/d-technolife-chords-532336", "UVERworld", "D-tecnolife", "JAPAN", "Soundtrack"),
    ("Bleach - Velonica", "https://tabs.ultimate-guitar.com/tab/aqua-timez/velonica-chords-839093", "Aqua Timez", "Velonica", "JAPAN", "Soundtrack"),
    ("Bleach - Alones", "https://tabs.ultimate-guitar.com/tab/aqua-timez/alones-chords-1487040", "Aqua Timez", "Alones", "JAPAN", "Soundtrack"),
    ("Fairy Tail - Snow Fairy", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/fairy-tail-snow-fairy-chords-1220404", "FUNKIST", "Snow Fairy", "JAPAN", "Soundtrack"),
    ("Death Note - The World", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/death-note-op-1-the-world-chords-643942", "Nightmare", "The World", "JAPAN", "Soundtrack"),
    ("Kenshi Yonezu - Lemon", "https://tabs.ultimate-guitar.com/tab/2384719", "Kenshi Yonezu", "Lemon", "JAPAN", "Soundtrack"),
    ("King Gnu - Hakujitsu", "https://tabs.ultimate-guitar.com/tab/2842238", "King Gnu", "Hakujitsu", "JAPAN", "Pop"),

    # 3. Disney Classics & Pixar
    ("The Little Mermaid - Part of Your World", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/the-little-mermaid-part-of-your-world-chords-978268", "Jodi Benson", "Part of Your World", "BARAT", "Soundtrack"),
    ("Cars - Life Is a Highway", "https://tabs.ultimate-guitar.com/tab/rascal-flatts/life-is-a-highway-chords-447451", "Rascal Flatts", "Life Is a Highway", "BARAT", "Soundtrack"),
    ("Brave - Touch the Sky", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/brave-touch-the-sky-chords-1163480", "Julie Fowlis", "Touch the Sky", "BARAT", "Soundtrack"),
    ("Pinocchio - When You Wish Upon A Star", "https://tabs.ultimate-guitar.com/tab/misc-cartoons/pinocchio-when-you-wish-upon-a-star-chords-1104666", "Cliff Edwards", "When You Wish Upon a Star", "BARAT", "Soundtrack"),

    # 4. K-Drama Hits
    ("Our Beloved Summer - Christmas Tree", "https://tabs.ultimate-guitar.com/tab/4000990", "V", "Christmas Tree", "KOREA", "Soundtrack"),
    ("Moon Lovers - For You", "https://tabs.ultimate-guitar.com/tab/exo-cbx/for-you-chords-2822150", "EXO-CBX", "For You", "KOREA", "Soundtrack"),
    ("Moon Lovers - Say Yes", "https://tabs.ultimate-guitar.com/tab/2034371", "Loco & Punch", "Say Yes", "KOREA", "Soundtrack"),
    ("Queen of Tears - Love You With All My Heart", "https://tabs.ultimate-guitar.com/tab/5205390", "Crush", "Love You With All My Heart", "KOREA", "Soundtrack"),
    ("Hospital Playlist 2 - Rain And You", "https://tabs.ultimate-guitar.com/tab/3759755", "Lee Mujin", "Rain and You", "KOREA", "Soundtrack"),
    ("Hospital Playlist - I Like You", "https://tabs.ultimate-guitar.com/tab/3795782", "Jo Jung Suk", "I Like You", "KOREA", "Soundtrack"),

    # 5. Indo Viral (Bernadya, Sal Priadi, Rio Clappy)
    ("Bernadya - Satu Bulan", "https://tabs.ultimate-guitar.com/tab/bernadya/satu-bulan-chords-5162968", "Bernadya", "Satu Bulan", "INDO", "Pop"),
    ("Bernadya - Kata Mereka Ini Berlebihan", "https://tabs.ultimate-guitar.com/tab/bernadya/kata-mereka-ini-berlebihan-chords-5172040", "Bernadya", "Kata Mereka Ini Berlebihan", "INDO", "Pop"),
    ("Bernadya - Kini Mereka Tahu", "https://tabs.ultimate-guitar.com/tab/bernadya/kini-mereka-tahu-chords-5272047", "Bernadya", "Kini Mereka Tahu", "INDO", "Pop"),
    ("Sal Priadi - Gala Bunga Matahari", "https://tabs.ultimate-guitar.com/tab/sal-priadi/gala-bunga-matahari-chords-5253435", "Sal Priadi", "Gala Bunga Matahari", "INDO", "Pop"),
    ("Sal Priadi - Dari Planet Lain", "https://tabs.ultimate-guitar.com/tab/sal-priadi/dari-planet-lain-chords-5249631", "Sal Priadi", "Dari Planet Lain", "INDO", "Pop"),
    ("Rio Clappy - Bunga Abadi", "https://tabs.ultimate-guitar.com/tab/rio-clappy/bunga-abadi-chords-5350137", "Rio Clappy", "Bunga Abadi", "INDO", "Pop"),
]

def harvest_batch_23():
    db_out = TOOLS_DIR / "catalog" / "db_batches" / "batch_23.db"
    if db_out.exists():
        db_out.unlink()
        
    conn = sqlite3.connect(db_out)
    c = conn.cursor()
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
            region TEXT NOT NULL,
            chords TEXT NOT NULL,
            chord_count INTEGER NOT NULL,
            line_count INTEGER NOT NULL,
            content TEXT NOT NULL,
            content_hash TEXT NOT NULL,
            scraped_at TEXT NOT NULL
        )
    """)
    conn.commit()

    total_scraped = 0
    print(f"=== 1. Scraping {len(UG_TARGETS)} UG targets for Batch 23 ===")
    for desc, url, artist, title, reg, cat in UG_TARGETS:
        print(f"Fetching: {desc}...", end="", flush=True)
        html_doc = fetch_html(url)
        if not html_doc:
            print(" [FAILED]")
            continue
        parsed = parse_ug(html_doc, url, artist, title, region=reg, category=cat)
        if parsed:
            try:
                c.execute("""
                    INSERT INTO songs (
                        source, source_url, title, artist, title_slug, artist_slug,
                        version_name, rating, votes, key_base, difficulty, category,
                        region, chords, chord_count, line_count, content, content_hash, scraped_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, parsed)
                conn.commit()
                total_scraped += 1
                print(f" [OK] ({parsed[8]} votes)")
            except sqlite3.IntegrityError:
                print(" [DUPLICATE]")
        else:
            print(" [PARSE FAILED]")
        time.sleep(0.3)

    print(f"\n=== 2. Pulling high-value missing Indo hits from chordtela.db ===")
    # Pull curated missing Indonesian gems from chordtela.db
    ct_db = Path(r"C:\KordApp-AGY\Catalog\chordtela.db")
    if ct_db.exists():
        conn_ct = sqlite3.connect(ct_db)
        c_ct = conn_ct.cursor()
        
        target_artists = [
            'Denny Caknan', 'Gildcoustic', 'Wahyu F Giri', 'Juicy Luicy', 
            'Mahalini', 'Guyon Waton', 'Happy Asmara', 'Ndarboy Genk', 'Feby Putri'
        ]
        
        pulled = 0
        for tart in target_artists:
            ct_rows = c_ct.execute("""
                SELECT source_url, title, artist, title_slug, artist_slug, category, region, chords, chord_count, line_count, content, scraped_at
                FROM songs 
                WHERE artist LIKE ? AND chord_count >= 3
                ORDER BY id ASC LIMIT 5
            """, (f"%{tart}%",)).fetchall()
            
            for row in ct_rows:
                s_url, t_title, t_artist, t_tslug, t_aslug, t_cat, t_reg, t_chords, t_ccnt, t_lcnt, t_cont, t_scraped = row
                
                # Check if already in batch_23
                exists = c.execute("SELECT count(*) FROM songs WHERE source_url = ?", (s_url,)).fetchone()[0]
                if exists:
                    continue
                
                content_hash = hashlib.md5(t_cont.encode('utf-8')).hexdigest()
                try:
                    c.execute("""
                        INSERT INTO songs (
                            source, source_url, title, artist, title_slug, artist_slug,
                            version_name, rating, votes, key_base, difficulty, category,
                            region, chords, chord_count, line_count, content, content_hash, scraped_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        "ChordTela", s_url, t_title, t_artist, t_tslug, t_aslug,
                        "Original", 4.8, 100, "", "Intermediate", "Pop",
                        "INDO", t_chords, t_ccnt, t_lcnt, t_cont, content_hash, t_scraped or datetime.datetime.now().isoformat()
                    ))
                    conn.commit()
                    pulled += 1
                except Exception as e:
                    pass
        conn_ct.close()
        print(f"Pulled {pulled} curated Indonesian hits into batch_23.db")
        total_scraped += pulled

    conn.close()
    print(f"\nBatch 23 generation finished! Total songs in batch_23.db: {total_scraped}")

if __name__ == '__main__':
    harvest_batch_23()
