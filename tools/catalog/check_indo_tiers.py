import sqlite3

conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()

# Check tier of the 39 ChordTela songs
ct_tiers = c.execute("SELECT tier, count(*) FROM songs WHERE source = 'ChordTela' GROUP BY tier").fetchall()
print('ChordTela songs tier distribution:', ct_tiers)

# Check all songs for Dere, Danilla, Gilga Sahid
indo_artists = ['dere', 'danilla', 'gilga-sahid']
for a in indo_artists:
    tiers = c.execute("SELECT tier, count(*) FROM songs WHERE artist_slug = ? GROUP BY tier", (a,)).fetchall()
    print(f"Artist {a} tiers:", tiers)

# Ensure all of them are in tier 'A'
c.execute("UPDATE songs SET tier = 'A' WHERE source = 'ChordTela' AND tier != 'A'")
c.execute("UPDATE songs SET tier = 'A' WHERE artist_slug IN ('dere', 'danilla', 'gilga-sahid') AND tier != 'A'")
conn.commit()

# Print total songs by tier
print("\n=== DISTRIBUSI TOTAL PER TIER ===")
for tier, count in c.execute("SELECT tier, count(*) FROM songs GROUP BY tier ORDER BY tier"):
    print(f"Tier {tier}: {count:,}")

conn.close()
