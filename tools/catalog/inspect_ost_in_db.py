import glob
import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

db_files = sorted(glob.glob('tools/catalog/db_batches/batch_*.db'))

def search_song(title_keyword, artist_keyword=""):
    print(f"\n--- Searching: '{title_keyword}' (artist: '{artist_keyword}') ---")
    results = []
    for db_path in db_files:
        conn = sqlite3.connect(db_path)
        c = conn.cursor()
        query = "SELECT artist, title, artist_slug, title_slug, source FROM songs WHERE 1=1"
        params = []
        if title_keyword:
            query += " AND title LIKE ?"
            params.append(f"%{title_keyword}%")
        if artist_keyword:
            query += " AND artist LIKE ?"
            params.append(f"%{artist_keyword}%")
        for r in c.execute(query, params).fetchall():
            results.append((r[0], r[1], r[2], r[3], r[4]))
        conn.close()
    if not results:
        print("  NOT FOUND in any batch!")
    else:
        for r in list(set(results))[:5]:
            print(f"  FOUND: {r[0]} - {r[1]} (slug: {r[2]} / {r[3]}) [{r[4]}]")


search_song("Let It Go", "Idina")
search_song("Danger Zone", "Kenny")
search_song("Lihatlah", "Sherina")
search_song("Beautiful", "Crush")
search_song("All About You", "Taeyeon")
search_song("Don't Worry", "Lee Juck")
search_song("A Million Dreams", "Hugh")
search_song("Never Enough", "Loren")
search_song("Shinzou wo", "Linked")
search_song("Unravel", "Ling")
search_song("Butter-Fly", "Koji")
search_song("Always Remember Us", "Lady Gaga")
search_song("Eye of the Tiger")
search_song("Ghostbusters")
search_song("See You Again")
search_song("Hero", "Chad")
search_song("Harta Berharga")
search_song("Tentang Seseorang")
search_song("Berdua Lebih Baik")
search_song("Kukira Kau Rumah")
