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

def search_ug(query: str):
    url = f"https://www.ultimate-guitar.com/search.php?title={urllib.parse.quote(query)}&type=300"
    req = urllib.request.Request(url, headers=headers)
    try:
        content = urllib.request.urlopen(req, timeout=10).read().decode('utf-8')
        m = re.search(r'class="js-store"\s+data-content="([^"]+)"', content)
        if m:
            data = json.loads(html.unescape(m.group(1)))
            results = data.get('store', {}).get('page', {}).get('data', {}).get('results', [])
            chord_results = [r for r in results if r.get('type') == 'Chords']
            if chord_results:
                chord_results.sort(key=lambda x: x.get('votes', 0), reverse=True)
                top = chord_results[0]
                return top.get('tab_url'), top.get('song_name'), top.get('artist_name'), top.get('votes', 0)
    except Exception as e:
        print(f"  [SEARCH ERR] {query}: {e}")
    return None

def parse_ug(html_text: str, url: str, artist: str, title: str, region: str = "BARAT", category: str = "Soundtrack"):
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

    content_hash = hashlib.sha256(sheet.encode('utf-8')).hexdigest()
    now_str = datetime.datetime.now(datetime.timezone.utc).isoformat()

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
        "category": category,
        "region": region,
        "chords": ",".join(unique_chords),
        "chord_count": len(unique_chords),
        "line_count": len(sheet.splitlines()),
        "content": sheet,
        "content_hash": content_hash,
        "scraped_at": now_str
    }

# ==========================================
# MASTER TARGET CURATION
# (Franchise ID, Display Title, Artist, Search Query, Category, Region, Role/Subtitle)
# ==========================================
TARGET_SONGS = [
    # --- 1. FROZEN 1 & 2 ---
    ("frozen", "Do You Want to Build a Snowman?", "Kristen Bell", "Frozen Snowman", "Soundtrack", "BARAT", "Frozen 1 Hit"),
    ("frozen", "For the First Time in Forever", "Kristen Bell & Idina Menzel", "Frozen First Time In Forever", "Soundtrack", "BARAT", "Frozen 1 Duet"),
    ("frozen", "Love Is an Open Door", "Kristen Bell & Santino Fontana", "Frozen Love Is An Open Door", "Soundtrack", "BARAT", "Frozen 1 Duet"),
    ("frozen", "In Summer", "Josh Gad", "Frozen In Summer", "Soundtrack", "BARAT", "Olaf Anthem"),
    ("frozen", "Fixer Upper", "Frozen Cast", "Frozen Fixer Upper", "Soundtrack", "BARAT", "Trolls Song"),
    ("frozen", "Show Yourself", "Idina Menzel & Evan Rachel Wood", "Frozen 2 Show Yourself", "Soundtrack", "BARAT", "Frozen 2 Climax"),
    ("frozen", "All Is Found", "Evan Rachel Wood", "Frozen 2 All Is Found", "Soundtrack", "BARAT", "Lullaby Theme"),
    ("frozen", "Some Things Never Change", "Frozen 2 Cast", "Frozen Some Things Never Change", "Soundtrack", "BARAT", "Frozen 2 Opening"),
    ("frozen", "Lost in the Woods", "Jonathan Groff", "Frozen 2 Lost In The Woods", "Soundtrack", "BARAT", "Kristoff Ballad"),
    ("frozen", "The Next Right Thing", "Kristen Bell", "Frozen Next Right Thing Kristen Bell", "Soundtrack", "BARAT", "Anna Ballad"),

    # --- 2. COCO ---
    ("coco", "Un Poco Loco", "Anthony Gonzalez & Gael Garcia Bernal", "Coco Un Poco Loco", "Soundtrack", "BARAT", "Performance Song"),
    ("coco", "Proud Corazon", "Anthony Gonzalez", "Coco Proud Corazon", "Soundtrack", "BARAT", "Finale Theme"),
    ("coco", "La Llorona", "Alanna Ubach & Antonio Sol", "Coco La Llorona", "Soundtrack", "BARAT", "Concert Scene"),

    # --- 3. MOANA ---
    ("moana", "You're Welcome", "Dwayne Johnson", "Moana Youre Welcome", "Soundtrack", "BARAT", "Maui Anthem"),
    ("moana", "Shiny", "Jemaine Clement", "Moana Shiny", "Soundtrack", "BARAT", "Tamatoa Song"),
    ("moana", "Where You Are", "Christopher Jackson & Rachel House", "Moana Where You Are", "Soundtrack", "BARAT", "Motunui Opening"),
    ("moana", "I Am Moana", "Rachel House & Aulii Cravalho", "Moana I Am Moana", "Soundtrack", "BARAT", "Song of the Ancestors"),
    ("moana", "We Know the Way", "Lin-Manuel Miranda & Opetaia Foa'i", "Moana We Know The Way", "Soundtrack", "BARAT", "Voyagers Theme"),

    # --- 4. ENCANTO ---
    ("encanto", "Surface Pressure", "Jessica Darrow", "Encanto Surface Pressure", "Soundtrack", "BARAT", "Luisa Song"),
    ("encanto", "What Else Can I Do?", "Diane Guerrero & Stephanie Beatriz", "Encanto What Else Can I Do", "Soundtrack", "BARAT", "Isabela Duet"),
    ("encanto", "Dos Oruguitas", "Sebastian Yatra", "Encanto Dos Oruguitas", "Soundtrack", "BARAT", "Emotional Lullaby"),
    ("encanto", "Waiting On A Miracle", "Stephanie Beatriz", "Encanto Waiting On A Miracle", "Soundtrack", "BARAT", "Mirabel Solo"),
    ("encanto", "The Family Madrigal", "Stephanie Beatriz & Encanto Cast", "Encanto Family Madrigal", "Soundtrack", "BARAT", "Opening Song"),

    # --- 5. TANGLED ---
    ("tangled", "Mother Knows Best", "Donna Murphy", "Tangled Mother Knows Best", "Soundtrack", "BARAT", "Mother Gothel Song"),
    ("tangled", "I've Got a Dream", "Tangled Cast", "Tangled Ive Got A Dream", "Soundtrack", "BARAT", "Snuggly Duckling Song"),

    # --- 6. MULAN & HERCULES & BEAUTY ---
    ("mulan", "I'll Make a Man Out of You", "Donny Osmond", "Mulan Ill Make A Man Out Of You", "Soundtrack", "BARAT", "Training Anthem"),
    ("hercules_pocahontas", "I Won't Say (I'm in Love)", "Susan Egan", "Hercules I Wont Say Im In Love", "Soundtrack", "BARAT", "Megara Anthem"),
    ("hercules_pocahontas", "Zero to Hero", "Hercules Cast", "Hercules Zero To Hero", "Soundtrack", "BARAT", "Gospel Theme"),
    ("beauty_and_the_beast", "Be Our Guest", "Beauty and the Beast Cast", "Beauty And The Beast Be Our Guest", "Soundtrack", "BARAT", "Lumiere Classic"),
    ("beauty_and_the_beast", "Gaston", "Jesse Corti & Richard White", "Beauty And The Beast Gaston", "Soundtrack", "BARAT", "Tavern Song"),
    ("lion_king", "Hakuna Matata", "Nathan Lane & Ernie Sabella", "Lion King Hakuna Matata", "Soundtrack", "BARAT", "Timon & Pumbaa Anthem"),
    ("lion_king", "I Just Can't Wait to Be King", "Jason Weaver", "Lion King I Just Cant Wait To Be King", "Soundtrack", "BARAT", "Young Simba"),

    # --- 7. SHREK 1 & 2 (NEW FRANCHISE!) ---
    ("shrek", "All Star", "Smash Mouth", "Smash Mouth All Star", "Soundtrack", "BARAT", "Shrek Opening"),
    ("shrek", "I'm a Believer", "Smash Mouth", "Smash Mouth Im A Believer", "Soundtrack", "BARAT", "Shrek Finale"),
    ("shrek", "Accidentally in Love", "Counting Crows", "Counting Crows Accidentally In Love", "Soundtrack", "BARAT", "Shrek 2 Theme"),
    ("shrek", "Holding Out for a Hero", "Frou Frou", "Frou Frou Holding Out For A Hero", "Soundtrack", "BARAT", "Shrek 2 Climax"),
    ("shrek", "Hallelujah", "Rufus Wainwright", "Rufus Wainwright Hallelujah", "Soundtrack", "BARAT", "Shrek Emotional Scene"),

    # --- 8. THE LORD OF THE RINGS & THE HOBBIT (NEW FRANCHISE!) ---
    ("lotr_hobbit", "I See Fire", "Ed Sheeran", "Ed Sheeran I See Fire", "Soundtrack", "BARAT", "Desolation of Smaug Theme"),
    ("lotr_hobbit", "May It Be", "Enya", "Enya May It Be", "Soundtrack", "BARAT", "Fellowship of the Ring Theme"),
    ("lotr_hobbit", "Into the West", "Annie Lennox", "Annie Lennox Into The West", "Soundtrack", "BARAT", "Return of the King Theme"),
    ("lotr_hobbit", "The Last Goodbye", "Billy Boyd", "Billy Boyd The Last Goodbye", "Soundtrack", "BARAT", "Battle of the Five Armies"),
    ("lotr_hobbit", "Misty Mountains", "Richard Armitage", "Misty Mountains Cold The Hobbit", "Soundtrack", "BARAT", "Dwarves Song"),

    # --- 9. TOP GUN & BARBIE & SPIDER-MAN ---
    ("top_gun", "I Ain't Worried", "OneRepublic", "OneRepublic I Aint Worried", "Soundtrack", "BARAT", "Maverick Beach Scene"),
    ("barbie", "I'm Just Ken", "Ryan Gosling", "Ryan Gosling Im Just Ken", "Soundtrack", "BARAT", "Ken Ballad"),
    ("barbie", "Dance the Night", "Dua Lipa", "Dua Lipa Dance The Night", "Soundtrack", "BARAT", "Party Theme"),
    ("spider_man", "Am I Dreaming", "Metro Boomin, A$AP Rocky, Roisee", "Metro Boomin Am I Dreaming", "Soundtrack", "BARAT", "Across the Spider-Verse"),
    ("spider_man", "Mona Lisa", "Dominic Fike", "Dominic Fike Mona Lisa", "Soundtrack", "BARAT", "Spider-Verse Theme"),
    ("spider_man", "Vindicated", "Dashboard Confessional", "Dashboard Confessional Vindicated", "Soundtrack", "BARAT", "Spider-Man 2 Theme"),

    # --- 10. NARUTO COMPLETE PACK ---
    ("naruto", "Hero's Come Back!!", "nobodyknows+", "naruto shippuden heros come back", "Soundtrack", "JAPAN", "Shippuden OP 1"),
    ("naruto", "Distance", "LONG SHOT PARTY", "long shot party distance", "Soundtrack", "JAPAN", "Shippuden OP 2"),
    ("naruto", "CLOSER", "Joe Inoue", "naruto shippuden closer", "Soundtrack", "JAPAN", "Shippuden OP 4"),
    ("naruto", "Diver", "NICO Touches the Walls", "naruto shippuden diver", "Soundtrack", "JAPAN", "Shippuden OP 8"),
    ("naruto", "Totsugeki Rock", "The Cro-Magnons", "naruto shippuden totsugeki rock", "Soundtrack", "JAPAN", "Shippuden OP 11"),
    ("naruto", "Tsuki no Ookisa", "Nogizaka46", "nogizaka46 tsuki no ookisa", "Soundtrack", "JAPAN", "Shippuden OP 14"),
    ("naruto", "Guren", "DOES", "does guren naruto", "Soundtrack", "JAPAN", "Shippuden OP 15"),
    ("naruto", "Kara no Kokoro", "Anly", "anly kara no kokoro", "Soundtrack", "JAPAN", "Shippuden OP 20"),
    ("naruto", "Kanashimi wo Yasashisa ni", "little by little", "little by little kanashimi wo yasashisa ni", "Soundtrack", "JAPAN", "Naruto OP 3"),
    ("naruto", "Seishun Kyousoukyoku", "Sambomaster", "sambomaster seishun kyousoukyoku", "Soundtrack", "JAPAN", "Naruto OP 5"),
    ("naruto", "Namikaze Satellite", "Snowkel", "naruto namikaze satellite", "Soundtrack", "JAPAN", "Naruto OP 7"),
    ("naruto", "Yura Yura", "Hearts Grow", "hearts grow yura yura", "Soundtrack", "JAPAN", "Naruto OP 9"),
    ("naruto", "Harmonia", "RYTHEM", "rythem harmonia", "Soundtrack", "JAPAN", "Naruto ED 2"),
    ("naruto", "ALIVE", "Raiko", "raiko alive", "Soundtrack", "JAPAN", "Naruto ED 4"),
    ("naruto", "Utakata Hanabi", "supercell", "supercell utakata hanabi", "Soundtrack", "JAPAN", "Shippuden ED 14"),
    ("naruto", "Kotoba no Iranai Yakusoku", "HoneyWorks", "honeyworks kotoba no iranai yakusoku", "Soundtrack", "JAPAN", "Shippuden ED 30"),
    ("naruto", "Pino to Amélie", "Huwie Ishizaki", "naruto pino to ameri", "Soundtrack", "JAPAN", "Shippuden ED 38"),
    ("naruto", "Hoshi no Utsuwa", "Sukima Switch", "sukima switch hoshi no utsuwa", "Soundtrack", "JAPAN", "The Last Movie Theme"),
    ("naruto", "Baton Road", "KANA-BOON", "kana boon baton road", "Soundtrack", "JAPAN", "Boruto OP 1"),

    # --- 11. ONE PIECE & DEMON SLAYER EXPANSION ---
    ("one_piece", "New Genesis (Shinjidai)", "Ado", "Ado Shinjidai", "Soundtrack", "JAPAN", "Film Red Theme"),
    ("one_piece", "Watashi wa Saikyou", "Ado", "Ado Watashi Wa Saikyou", "Soundtrack", "JAPAN", "Film Red Insert"),
    ("one_piece", "Hikari E", "The Babystars", "The Babystars Hikari E", "Soundtrack", "JAPAN", "Opening 3"),
    ("demon_slayer", "Homura", "LiSA", "LiSA Homura Mugen Train", "Soundtrack", "JAPAN", "Mugen Train Theme"),
    ("demon_slayer", "Kizuna no Kiseki", "MAN WITH A MISSION x milet", "man with a mission kizuna no kiseki", "Soundtrack", "JAPAN", "Swordsmith Village OP"),

    # --- 12. K-DRAMA EXPANSION (CLOY & QUEEN OF TEARS!) ---
    ("cloy", "Give You My Heart", "IU", "IU Give You My Heart", "Soundtrack", "KOREA", "Crash Landing on You"),
    ("cloy", "Here I Am Again", "Yerin Baek", "Yerin Baek Here I Am Again", "Soundtrack", "KOREA", "Crash Landing on You"),
    ("cloy", "Flower", "Yoon Mi-rae", "Yoon Mi Rae Flower CLOY", "Soundtrack", "KOREA", "Crash Landing on You"),
    ("queen_of_tears", "The Reasons of My Smiles", "BSS (SEVENTEEN)", "BSS The Reasons Of My Smiles", "Soundtrack", "KOREA", "Queen of Tears OP"),
    ("queen_of_tears", "Hold Me Back", "Heize", "Heize Hold Me Back Queen Of Tears", "Soundtrack", "KOREA", "Queen of Tears Theme"),
    ("dots", "You Are My Everything", "Gummy", "Gummy You Are My Everything", "Soundtrack", "KOREA", "Descendants of the Sun"),
    ("dots", "Always", "Yoon Mi-rae", "Yoon Mi Rae Always DOTS", "Soundtrack", "KOREA", "Descendants of the Sun"),
]

def init_db(db_path: str):
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("""
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
    return conn

def run_harvest():
    db_out_path = r"C:\KordApp-AGY\tools\catalog\db_batches\batch_22.db"
    conn_out = init_db(db_out_path)
    cur_out = conn_out.cursor()

    # Juga cek master DB untuk yang sudah ada
    master_db = r"C:\KordApp-AGY\tools\catalog\full_merged_catalog.db"
    conn_master = sqlite3.connect(master_db)
    cur_m = conn_master.cursor()

    already_in_master = 0
    scraped_success = 0
    failed = 0

    print(f"=== MEMULAI HARVEST BATCH 22 ({len(TARGET_SONGS)} LAGU TARGET) ===")
    
    for idx, item in enumerate(TARGET_SONGS, 1):
        franchise_id, title, artist, query, category, region, role = item
        t_slug = to_slug(title)
        a_slug = to_slug(artist)

        # 1. Cek di master catalog apakah sudah ada
        m_row = cur_m.execute("SELECT id, source_url FROM songs WHERE artist_slug = ? AND title_slug = ?", (a_slug, t_slug)).fetchone()
        if not m_row:
            # Cari loose by title
            m_row = cur_m.execute("SELECT id, source_url FROM songs WHERE title_slug = ?", (t_slug,)).fetchone()

        if m_row:
            print(f"[{idx}/{len(TARGET_SONGS)}] [SUDAH ADA DI MASTER] {artist} - {title} (ID {m_row[0]})")
            already_in_master += 1
            continue

        # Cek apakah sudah ada di batch_22.db
        b_row = cur_out.execute("SELECT id FROM songs WHERE title_slug = ?", (t_slug,)).fetchone()
        if b_row:
            print(f"[{idx}/{len(TARGET_SONGS)}] [SUDAH ADA DI BATCH 22] {artist} - {title}")
            continue

        print(f"[{idx}/{len(TARGET_SONGS)}] [SEARCHING UG] '{query}'...")
        res = search_ug(query)
        if not res:
            print(f"  -> TIDAK DITEMUKAN: {query}")
            failed += 1
            time.sleep(0.4)
            continue

        tab_url, ug_song, ug_art, ug_votes = res
        print(f"  -> FOUND: {ug_art} - {ug_song} ({ug_votes} votes) -> {tab_url}")

        html_content = fetch_html(tab_url)
        if not html_content:
            failed += 1
            continue

        parsed = parse_ug(html_content, tab_url, artist, title, region=region, category=category)
        if not parsed:
            print(f"  -> PARSE GAGAL: {tab_url}")
            failed += 1
            continue

        try:
            cur_out.execute("""
                INSERT OR IGNORE INTO songs (
                    source, source_url, title, artist, title_slug, artist_slug,
                    version_name, rating, votes, key_base, difficulty, category,
                    region, chords, chord_count, line_count, content, content_hash, scraped_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                parsed["source"], parsed["source_url"], parsed["title"], parsed["artist"],
                parsed["title_slug"], parsed["artist_slug"], parsed["version_name"],
                parsed["rating"], parsed["votes"], parsed["key_base"], parsed["difficulty"],
                parsed["category"], parsed["region"], parsed["chords"], parsed["chord_count"],
                parsed["line_count"], parsed["content"], parsed["content_hash"], parsed["scraped_at"]
            ))
            conn_out.commit()
            scraped_success += 1
            print(f"  [SAVED] {artist} - {title} ({parsed['chord_count']} chords, {parsed['line_count']} lines)")
        except Exception as e:
            print(f"  -> DB INSERT ERROR: {e}")
            failed += 1

        time.sleep(0.5)

    conn_master.close()
    conn_out.close()

    print("\n=== RINGKASAN HARVEST BATCH 22 ===")
    print(f"Sudah ada di Master Catalog : {already_in_master}")
    print(f"Berhasil di-Scrape & Simpan : {scraped_success}")
    print(f"Gagal / Tidak ditemukan     : {failed}")

if __name__ == "__main__":
    run_harvest()
