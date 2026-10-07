import sqlite3

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()
c.execute("SELECT COUNT(*) FROM songs")
print("Total song versions:", c.fetchone()[0])
c.execute("SELECT COUNT(DISTINCT artist_slug) FROM songs")
print("Total artists:", c.fetchone()[0])

print("\n--- Current getOstSongs results (first 25) ---")
c.execute("""
    SELECT artist, title, source_url
    FROM songs 
    WHERE lower(title) LIKE '%(ost%' OR lower(title) LIKE '%ost.%' 
       OR lower(title) LIKE '%soundtrack%' OR lower(title) LIKE '%ost %'
       OR lower(source_url) LIKE '%-ost-%'
    GROUP BY artist_slug, title_slug
    ORDER BY title ASC
    LIMIT 25
""")
for r in c.fetchall():
    print(r)
