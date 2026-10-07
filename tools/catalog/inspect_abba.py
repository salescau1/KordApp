import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

rows = c.execute("""
    SELECT id, source, title, title_slug, version_name, source_url
    FROM songs 
    WHERE lower(artist) LIKE '%abba%' AND (lower(title) LIKE '%dancing queen%' OR title_slug LIKE '%dancing-queen%')
""").fetchall()

print(f"Found {len(rows)} versions of Dancing Queen by ABBA:")
for r in rows:
    print(f"ID: {r[0]} | Source: {r[1]} | Title: {r[2]} | TitleSlug: {r[3]} | Version: {r[4]}")
    print(f"  URL: {r[5]}\n")

