import sqlite3

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

TARGETS = [
    ("The Vines", "the-vines"),
    ("The Hives", "the-hives"),
    ("The Fratellis", "the-fratellis"),
    ("Wolfmother", "wolfmother"),
    ("The Darkness", "the-darkness"),
    ("The Subways", "the-subways"),
    ("Caesars", "caesars"),
    ("Mando Diao", "mando-diao"),
    ("Matchbox 20", "matchbox-20"),
    ("Velvet Revolver", "velvet-revolver"),
    ("Theory of a Deadman", "theory-of-a-deadman"),
    ("A Rocket to the Moon", "a-rocket-to-the-moon"),
    ("All Time Low", "all-time-low"),
    ("Bowling for Soup", "bowling-for-soup"),
    ("Maneskin", "maneskin"),
    ("Royal Blood", "royal-blood"),
    ("Greta Van Fleet", "greta-van-fleet"),
    ("Dere", "dere"),
    ("Danilla", "danilla"),
    ("Gilga Sahid", "gilga-sahid"),
]

print("=== VERIFIKASI 20 ARTIS BATCH 24 DI MASTER DB ===")
total_artist_songs = 0
for name, slug in TARGETS:
    c.execute("""
        SELECT count(*), count(DISTINCT title_slug)
        FROM songs
        WHERE artist_slug = ? OR lower(artist) = ?
    """, (slug, name.lower()))
    total, unique_titles = c.fetchone()
    total_artist_songs += total

    # Breakdown per tier
    c.execute("""
        SELECT tier, count(*)
        FROM songs
        WHERE artist_slug = ? OR lower(artist) = ?
        GROUP BY tier
        ORDER BY tier
    """, (slug, name.lower()))
    tiers = dict(c.fetchall())

    # Breakdown per source
    c.execute("""
        SELECT source, count(*)
        FROM songs
        WHERE artist_slug = ? OR lower(artist) = ?
        GROUP BY source
        ORDER BY source
    """, (slug, name.lower()))
    sources = dict(c.fetchall())

    t_str = ", ".join(f"Tier {k}: {v}" for k, v in sorted(tiers.items()))
    s_str = ", ".join(f"{k}: {v}" for k, v in sorted(sources.items()))
    print(f"{name:20s} | Total: {total:3d} ({unique_titles:2d} titles) | {t_str:25s} | {s_str}")

print(f"\nTotal songs across all 20 artists: {total_artist_songs}")

print("\n--- Pengecekan Integritas Tier & Region ---")
c.execute("SELECT count(*) FROM songs WHERE region = 'INDO' AND tier = 'C'")
indo_c = c.fetchone()[0]
print(f"Lagu INDO di Tier C: {indo_c} (harus 0)")

c.execute("PRAGMA user_version")
u_ver = c.fetchone()[0]
print(f"PRAGMA user_version: {u_ver} (harus 6)")

c.execute("SELECT identity_hash FROM room_master_table WHERE id = 42")
i_hash = c.fetchone()[0]
print(f"Room identity_hash: {i_hash} (harus 'a69e31afaf7d1570cec628f49411725a')")

conn.close()
