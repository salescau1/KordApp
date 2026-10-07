import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

def search_song(artist_q, title_q):
    c.execute("""
        SELECT artist, artist_slug, title, title_slug, COUNT(*) 
        FROM songs 
        WHERE (lower(artist) LIKE ? OR lower(artist_slug) LIKE ?)
          AND (lower(title) LIKE ? OR lower(title_slug) LIKE ?)
        GROUP BY artist_slug, title_slug
        LIMIT 5
    """, (f"%{artist_q}%", f"%{artist_q}%", f"%{title_q}%", f"%{title_q}%"))
    return c.fetchall()

test_queries = [
    ("melly", "ada apa dengan cinta"),
    ("melly", "bimbang"),
    ("melly", "ku bahagia"),
    ("sherina", "lihatlah"),
    ("nidji", "laskar pelangi"),
    ("ipang", "sahabat kecil"),
    ("rossa", "ayat ayat cinta"),
    ("once", "dealova"),
    ("bunga citra lestari", "cinta sejati"),
    ("maudy ayunda", "perahu kertas"),
    ("hindia", "secukupnya"),
    ("fiersa besari", "pelukku"),
    ("radwimps", "sparkle"),
    ("radwimps", "zenzenzense"),
    ("radwimps", "nandemonaiya"),
    ("lisa", "gurenge"),
    ("lisa", "homura"),
    ("kana-boon", "silhouette"),
    ("flow", "go"),
    ("flow", "sign"),
    ("celine dion", "my heart will go on"),
    ("lady gaga", "shallow"),
    ("aerosmith", "miss a thing"),
    ("billie eilish", "what was i made for"),
    ("christina perri", "thousand years"),
]

print(f"{'ARTIST QUERY':<20} | {'TITLE QUERY':<20} | {'MATCH FOUND'}")
print("-" * 75)
for a_q, t_q in test_queries:
    matches = search_song(a_q, t_q)
    if matches:
        first = matches[0]
        print(f"{a_q:<20} | {t_q:<20} | {first[0]} - {first[2]} ({first[1]} / {first[3]}) [{first[4]} ver]")
    else:
        print(f"{a_q:<20} | {t_q:<20} | [NO MATCH]")
