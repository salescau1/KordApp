import sqlite3
import hashlib
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

conn_ct = sqlite3.connect('Catalog/chordtela.db')
c_ct = conn_ct.cursor()

conn_master = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c_m = conn_master.cursor()

# 1. Total songs in chordtela.db
total_ct = c_ct.execute("SELECT count(*) FROM songs").fetchone()[0]
print(f"Total songs in chordtela.db: {total_ct}")

# 2. Duplicate URLs with full_merged_catalog.db
master_urls = set(r[0] for r in c_m.execute("SELECT source_url FROM songs").fetchall())
ct_urls = set(r[0] for r in c_ct.execute("SELECT source_url FROM songs").fetchall())
overlap_urls = master_urls.intersection(ct_urls)
print(f"Overlapping URLs between master and chordtela: {len(overlap_urls)}")

# 3. Check sample rows in chordtela
print("\nSample 5 songs from chordtela.db:")
for r in c_ct.execute("SELECT id, title, artist, title_slug, artist_slug, category, region, chords, chord_count, line_count, scraped_at FROM songs LIMIT 5"):
    print(r)

# 4. Check artists in chordtela (ALL CAPS or Mixed Case?)
sample_artists = [r[0] for r in c_ct.execute("SELECT DISTINCT artist FROM songs LIMIT 20").fetchall()]
print(f"\nSample 20 artists:\n{sample_artists}")

# 5. Check if content has brackets
sample_content = c_ct.execute("SELECT content FROM songs WHERE id = 1").fetchone()[0]
print("\nSample content snippet (ID 1):")
print(sample_content[:200])

# 6. Check content_hash in master_db: how is it computed?
sample_master = c_m.execute("SELECT content, content_hash FROM songs LIMIT 1").fetchone()
computed_sha256 = hashlib.sha256(sample_master[0].encode('utf-8')).hexdigest()
computed_md5 = hashlib.md5(sample_master[0].encode('utf-8')).hexdigest()
print(f"\nMaster hash comparison:")
print(f"  Stored:   {sample_master[1]}")
print(f"  SHA256:   {computed_sha256}")
print(f"  MD5:      {computed_md5}")

conn_ct.close()
conn_m = conn_master.close()
