import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()

print("Existing ChordTela in master:")
for r in c.execute("SELECT id, source, source_url, title, artist, version_name, rating, votes, difficulty, category, region, chord_count, line_count FROM songs WHERE source = 'ChordTela' LIMIT 3"):
    print(r)

print("\nSongs count by region:")
for r in c.execute("SELECT region, count(*) FROM songs GROUP BY region"):
    print(r)

print("\nGT version_names:")
for r in c.execute("SELECT version_name, count(*) FROM songs WHERE source = 'GT' GROUP BY version_name"):
    print(r)

print("\nUG version_names sample:")
for r in c.execute("SELECT version_name, count(*) FROM songs WHERE source = 'UG' GROUP BY version_name ORDER BY count(*) DESC LIMIT 10"):
    print(r)

conn.close()

conn_ct = sqlite3.connect('Catalog/chordtela.db')
c_ct = conn_ct.cursor()
print("\nChordtela sample:")
for r in c_ct.execute("SELECT id, source_url, title, artist, region, chord_count, line_count FROM songs LIMIT 3"):
    print(r)
conn_ct.close()
