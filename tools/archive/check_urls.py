import sqlite3

for db_name, path in [('UG', r'C:\ChordApp\ug_crawler\data\ug\ug_catalog.db'), ('GT', r'C:\ChordApp\guitartuna_crawler\guitartuna_merged.db')]:
    conn = sqlite3.connect(path)
    cur = conn.cursor()
    print(f"=== {db_name} SAMPLE URLS ===")
    cur.execute("SELECT artist, title, source_url FROM songs WHERE artist_slug IN ('queen', 'adele') LIMIT 4")
    for r in cur.fetchall():
        print(r)
    conn.close()
