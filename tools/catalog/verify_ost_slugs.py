import glob
import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

db_files = sorted(glob.glob('tools/catalog/db_batches/batch_*.db'))
catalog = {} # (clean_artist, clean_title) -> (artist_slug, title_slug, artist, title)

for db_path in db_files:
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    try:
        for r in c.execute("SELECT artist, title, artist_slug, title_slug FROM songs"):
            art = r[0].lower().strip()
            tit = r[1].lower().strip()
            catalog[(art, tit)] = (r[2], r[3], r[0], r[1])
            # also index by slug
            catalog[(r[2], r[3])] = (r[2], r[3], r[0], r[1])
    except Exception as e:
        print(f"Err {db_path}: {e}")
    conn.close()

print(f"Catalog indexed {len(catalog)} entries.")

def check(art, tit):
    # Direct match
    k = (art.lower().strip(), tit.lower().strip())
    if k in catalog:
        m = catalog[k]
        print(f"  FOUND: {m[2]} - {m[3]} -> slug: (\"{m[0]}\", \"{m[1]}\")")
        return m[0], m[1]
    # Fuzzy match
    found = []
    for (a, t), v in catalog.items():
        if tit.lower() in t and (art.lower() in a or not art):
            found.append(v)
    if found:
        # sort by shortest title
        found.sort(key=lambda x: len(x[3]))
        m = found[0]
        print(f"  FUZZY: {m[2]} - {m[3]} -> slug: (\"{m[0]}\", \"{m[1]}\")")
        return m[0], m[1]
    print(f"  NOT FOUND: {art} - {tit}")
    return None, None

print("\n--- Checking Billy Crudup & Rock ---")
check("Billy Crudup", "Sing Along")
check("Billy Crudup", "Real Friends")
check("Billy Crudup", "Stay With You")
check("Billy Crudup", "Over Your Shoulder")
check("Billy Crudup", "Home")
check("Billy Crudup", "Asshole")
check("Stillwater", "Fever Dog")
check("Elton John", "Tiny Dancer")

print("\n--- Checking Anime ---")
check("Hiroshi Kitadani", "We Are!")
check("BOYSTYLE", "Kokoro no Chizu")
check("Maki Otsuki", "Memories")
check("Ado", "New Genesis")
check("Koji Wada", "Butter-Fly")
check("Ayumi Miyazaki", "Brave Heart")
check("Field of View", "Dan Dan Kokoro Hikareteku")
check("Hironobu Kageyama", "Cha-La Head-Cha-La")
check("TK from Ling Tosite Sigure", "Unravel")
check("BoA", "Every Heart")
check("T.M.Revolution", "Heart of Sword")
check("Judy and Mary", "Sobakasu")
check("BAAD", "Kimi ga Suki da to Sakebitai")
check("WANDS", "Sekai ga Owaru Made wa")
check("Matsuko Mawatari", "Hohoemi no Bakudan")
check("Linked Horizon", "Shinzou wo Sasageyo!")
check("Linked Horizon", "Guren no Yumiya")
check("Ai Higuchi", "Akuma no Ko")
check("RADWIMPS", "Zenzenzense")
check("Ikimono-Gakari", "Hotaru no Hikari")
check("7!!", "Lovers")
check("Motohiro Hata", "Toumei Datta Sekai")
check("Akeboshi", "Wind")
check("Akeboshi", "Yellow Moon")

print("\n--- Checking Disney & Hollywood ---")
check("Idina Menzel", "Let It Go")
check("Idina Menzel", "Into the Unknown")
check("Peabo Bryson", "A Whole New World")
check("Randy Newman", "You've Got A Friend In Me")
check("Mandy Moore", "I See The Light")
check("Mandy Moore", "When Will My Life Begin")
check("Auli'i Cravalho", "How Far I'll Go")
check("Lin-Manuel Miranda", "We Don't Talk About Bruno")
check("Michael Bolton", "Go The Distance")
check("Vanessa Williams", "Colors Of The Wind")
check("Anthony Gonzalez", "Remember Me")
check("Shakira", "Try Everything")
check("Hugh Jackman", "A Million Dreams")
check("Loren Allred", "Never Enough")
check("High School Musical Cast", "Breaking Free")
check("High School Musical Cast", "Start of Something New")
check("Kenny Loggins", "Danger Zone")
check("Berlin", "Take My Breath Away")
check("Lady Gaga", "Always Remember Us This Way")
check("Chad Kroeger", "Hero")
check("Survivor", "Eye of the Tiger")
check("Ray Parker Jr.", "Ghostbusters")
check("Wiz Khalifa", "See You Again")
check("Post Malone", "Sunflower")

print("\n--- Checking Indo & K-Drama ---")
check("Peterpan", "Tak Bisakah")
check("Peterpan", "Menunggu Pagi")
check("Peterpan", "Kukatakan Dengan Indah")
check("Chrisye", "Badai Pasti Berlalu")
check("Chrisye", "Pelangi")
check("Sherina Munaf", "Lihatlah Lebih Dekat")
check("Sherina Munaf", "Persahabatan")
check("Sherina Munaf", "Jagoan")
check("Anda", "Tentang Seseorang")
check("Amigdala", "Kukira Kau Rumah")
check("Melly Goeslaw", "Ada Apa Dengan Cinta")
check("Acha Septriasa", "Sampai Menutup Mata")
check("Irwansyah", "Pencinta Wanita")
check("The Panasdalam Bank", "Dulu Kita Masih Remaja")
check("Lee Juck", "Don't Worry")
check("Gummy", "Remember Me")
check("Crush", "Beautiful")
check("Chanyeol", "Stay With Me")
check("Ailee", "I Will Go to You Like the First Snow")
check("Gaho", "Start Over")
