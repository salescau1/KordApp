import sqlite3

conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()

artists_to_test = ['Jet', 'The Vines', 'The Hives', 'Matchbox 20', 'Dere', 'Danilla', 'Gilga Sahid', 'Maneskin', 'The Darkness', 'All Time Low', 'Bowling for Soup']
print("=== VERIFIKASI PENCARIAN ARTIS BARU ===")
for a in artists_to_test:
    res = c.execute("SELECT artist, count(*) FROM songs WHERE artist_slug = ? OR artist = ? GROUP BY artist", (a.lower().replace(' ', '-'), a)).fetchall()
    print(f"Artis '{a}': {res}")

print("\n=== VERIFIKASI LAGU HITS SPESIFIK ===")
hits = [
    ("Jet", "Look What You've Done"),
    ("The Vines", "Get Free"),
    ("The Hives", "Hate To Say I Told You So"),
    ("Matchbox 20", "3AM"),
    ("A Rocket to the Moon", "Like We Used To"),
    ("Dere", "Kota"),
    ("Gilga Sahid", "Nemen")
]
for artist, title in hits:
    row = c.execute("SELECT source, artist, title, tier, version_name FROM songs WHERE artist LIKE ? AND title LIKE ? LIMIT 1", (f"%{artist}%", f"%{title}%")).fetchone()
    print(f"Hit '{artist} - {title}': {row}")

conn.close()
