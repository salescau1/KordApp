import re
import sqlite3

db_path = "tools/catalog/full_merged_catalog.db"
conn = sqlite3.connect(db_path)
c = conn.cursor()

# Verify user_version
c.execute("PRAGMA user_version")
uv = c.fetchone()[0]
print(f"Database PRAGMA user_version: {uv} (Must be 5)")

# Parse OstCatalogData.kt
with open("app/src/main/java/com/chordapp/ui/screens/explore/OstCatalogData.kt", "r", encoding="utf-8") as f:
    content = f.read()

pattern = re.compile(r'OstSongItem\(\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"')
matches = pattern.findall(content)

print(f"\nFound {len(matches)} total OST song entries in OstCatalogData.kt")

missing = []
found = 0
for title, artist, artist_slug, title_slug in matches:
    row = c.execute("SELECT count(*) FROM songs WHERE artist_slug = ? AND title_slug = ?", (artist_slug, title_slug)).fetchone()[0]
    if row > 0:
        found += 1
    else:
        # Check if maybe just title_slug or artist_slug is slightly different
        fuzzy = c.execute("SELECT artist_slug, title_slug FROM songs WHERE title_slug = ? LIMIT 1", (title_slug,)).fetchone()
        missing.append((title, artist, artist_slug, title_slug, fuzzy))

print(f"Matched in database: {found}/{len(matches)} ({found/len(matches)*100:.1f}%)")

if missing:
    print(f"\nMissing {len(missing)} songs:")
    for t, a, aslug, tslug, fz in missing:
        print(f" - {t} by {a} (wanted: {aslug} / {tslug}) -> fuzzy suggestion: {fz}")
else:
    print("\nALL SONGS IN OSTCATALOGDATA ARE 100% MATCHED IN THE CATALOG DATABASE!")

conn.close()
