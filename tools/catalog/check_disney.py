import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

disney_searches = [
    ("phil collins", "youll be in my heart"),
    ("phil collins", "strangers like me"),
    ("elton john", "can you feel the love tonight"),
    ("elton john", "circle of life"),
    ("shakira", "try everything"),
    ("celine dion", "beauty and the beast"),
    ("disney", "let it go"),
    ("disney", "whole new world"),
    ("mandy moore", "i see the light"),
    ("christina aguilera", "reflection"),
    ("randy newman", "youve got a friend in me"),
    ("michael bolton", "go the distance"),
    ("vanessa williams", "colors of the wind"),
    ("lin-manuel miranda", "we dont talk about bruno"),
    ("encanto", "we dont talk about bruno"),
    ("moana", "how far ill go"),
]

print("--- Checking Disney songs in existing full_merged_catalog.db ---")
for a, t in disney_searches:
    c.execute("""
        SELECT artist, title, source_url 
        FROM songs 
        WHERE (lower(artist) LIKE ? OR lower(artist_slug) LIKE ?)
          AND (lower(title) LIKE ? OR lower(title_slug) LIKE ?)
        LIMIT 1
    """, (f"%{a}%", f"%{a}%", f"%{t}%", f"%{t}%"))
    r = c.fetchone()
    if r:
        print(f"[FOUND] {r[0]} - {r[1]}")
    else:
        print(f"[MISS] {a} - {t}")
