import glob
import re
import sqlite3
import sys

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

# 1. Parse franchises from OstCatalogData.kt
with open('app/src/main/java/com/chordapp/ui/screens/explore/OstCatalogData.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

current_cat = ""
current_id = ""
current_title = ""
current_songs = []
franchises = []

for line in lines:
    line_s = line.strip()
    m_id = re.search(r'id\s*=\s*"([^"]+)"', line_s)
    if m_id:
        current_id = m_id.group(1)
    m_t = re.search(r'title\s*=\s*"([^"]+)"', line_s)
    if m_t and not current_title:
        current_title = m_t.group(1)
    m_c = re.search(r'category\s*=\s*"([^"]+)"', line_s)
    if m_c:
        current_cat = m_c.group(1)
    m_song = re.search(r'OstSongItem\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"', line_s)
    if m_song:
        current_songs.append({
            'title': m_song.group(1),
            'artist': m_song.group(2),
            'artist_slug': m_song.group(3),
            'title_slug': m_song.group(4)
        })
    if line_s.startswith(')') and current_id and current_title:
        franchises.append({
            'id': current_id,
            'title': current_title,
            'category': current_cat,
            'songs': current_songs
        })
        current_id = ""
        current_title = ""
        current_songs = []

print(f"Total franchises in OstCatalogData.kt: {len(franchises)}")
total_songs = sum(len(f['songs']) for f in franchises)
print(f"Total songs across all franchises: {total_songs}")

# 2. Check song availability in full_merged_catalog.db
conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()
all_catalog_songs = set()
rows = c.execute("SELECT artist_slug, title_slug FROM songs").fetchall()
for aslug, tslug in rows:
    if aslug and tslug:
        all_catalog_songs.add((aslug.lower().strip(), tslug.lower().strip()))
conn.close()


print(f"Total unique (artist_slug, title_slug) in DB batches: {len(all_catalog_songs)}")

missing = []
found = []
for f in franchises:
    for s in f['songs']:
        key = (s['artist_slug'].lower().strip(), s['title_slug'].lower().strip())
        if key in all_catalog_songs:
            found.append((f['title'], s['artist'], s['title']))
        else:
            missing.append((f['title'], s['artist'], s['title'], s['artist_slug'], s['title_slug']))

print(f"\nSongs Found in DB: {len(found)} / {total_songs} ({len(found)/total_songs*100:.1f}%)")
print(f"Songs Missing from DB: {len(missing)} / {total_songs} ({len(missing)/total_songs*100:.1f}%)")
if missing:
    print("\nMissing Songs List:")
    for ftitle, art, stitle, aslug, tslug in missing:
        print(f"  [{ftitle}] {art} - {stitle} ({aslug} / {tslug})")
