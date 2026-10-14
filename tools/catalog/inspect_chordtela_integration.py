import sqlite3
import hashlib
from pathlib import Path

repo_dir = Path(r"C:\KordApp-AGY")
ct_db_path = repo_dir / "Catalog" / "chordtela.db"
master_db_path = repo_dir / "tools" / "catalog" / "full_merged_catalog.db"

conn_ct = sqlite3.connect(ct_db_path)
c_ct = conn_ct.cursor()

c_ct.execute("PRAGMA table_info(songs)")
cols = c_ct.fetchall()
print("ChordTela columns:")
for col in cols:
    print(" ", col)

c_ct.execute("SELECT count(*) FROM songs")
total_ct = c_ct.fetchone()[0]
print(f"Total songs in chordtela.db: {total_ct}")

c_ct.execute("SELECT id, source_url, title, artist, title_slug, artist_slug, region, chords, chord_count, line_count, content, scraped_at FROM songs LIMIT 3")
rows = c_ct.fetchall()
print("\nSample rows from chordtela.db:")
for r in rows:
    id_, url, title, artist, t_slug, a_slug, region, chords, chord_count, line_count, content, scraped_at = r
    print(f"ID {id_}: [{artist}] - [{title}] (region={region}, chords={chord_count}, lines={line_count})")
    print(f"  URL: {url}")
    print(f"  First 100 chars of content:\n  {repr(content[:100])}")

conn_ct.close()

# Check how many of these URLs already exist in master_db
conn_master = sqlite3.connect(master_db_path)
c_master = conn_master.cursor()
c_master.execute("SELECT count(*) FROM songs WHERE source = 'ChordTela'")
print(f"\nExisting ChordTela songs in master_db: {c_master.fetchone()[0]}")
conn_master.close()
