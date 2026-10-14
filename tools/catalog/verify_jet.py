import sqlite3

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

print("=== VERIFIKASI DATA JET DI MASTER CATALOG ===")
c.execute("SELECT count(*) FROM songs WHERE artist_slug = 'jet'")
total_jet = c.fetchone()[0]
print(f"Total versi lagu Jet: {total_jet}")

c.execute("SELECT count(DISTINCT title_slug) FROM songs WHERE artist_slug = 'jet'")
unique_titles = c.fetchone()[0]
print(f"Total judul unik Jet: {unique_titles}")

print("\n--- Distribusi per Sumber & Tier ---")
c.execute("""
    SELECT source, tier, count(*)
    FROM songs 
    WHERE artist_slug = 'jet'
    GROUP BY source, tier
    ORDER BY source, tier
""")
for r in c.fetchall():
    print(f"  {r[0]} - Tier {r[1]}: {r[2]} lagu")

print("\n--- Breakdown Version Name Jet ---")
c.execute("""
    SELECT version_name, tier, count(*)
    FROM songs 
    WHERE artist_slug = 'jet'
    GROUP BY version_name, tier
    ORDER BY tier, count(*) DESC
""")
for r in c.fetchall():
    print(f"  [{r[1]}] {r[0]}: {r[2]} lagu")

print("\n--- Verifikasi Integritas SQLite Room ---")
user_ver = c.execute("PRAGMA user_version").fetchone()[0]
hash_val = c.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]
print(f"  PRAGMA user_version: {user_ver} (harus 6)")
print(f"  Room identity_hash: {hash_val} (harus 'a69e31afaf7d1570cec628f49411725a')")

print("\n--- Daftar Lengkap Lagu Jet Berdasarkan Judul ---")
c.execute("""
    SELECT title, title_slug, count(*) as ver_count,
           GROUP_CONCAT(source || ' (' || version_name || ', Tier ' || tier || ')', ' | ') as versions
    FROM songs
    WHERE artist_slug = 'jet'
    GROUP BY title_slug
    ORDER BY title
""")
rows = c.fetchall()
for i, r in enumerate(rows, 1):
    print(f"{i:2d}. {r[0]} [{r[1]}] ({r[2]} versi):\n    -> {r[3]}")

conn.close()
