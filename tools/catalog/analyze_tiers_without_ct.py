import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()

# Check non-ChordTela count
total_no_ct = c.execute("SELECT count(*) FROM songs WHERE source != 'ChordTela'").fetchone()[0]
print(f"Total songs (excluding ChordTela): {total_no_ct}")

ct_count = c.execute("SELECT count(*) FROM songs WHERE source = 'ChordTela'").fetchone()[0]
print(f"ChordTela songs currently in DB to remove: {ct_count}")

print("\nSongs by Source:")
for r in c.execute("SELECT source, count(*) FROM songs WHERE source != 'ChordTela' GROUP BY source"):
    print(f"  {r[0]}: {r[1]:,}")

print("\nGT Versions:")
for r in c.execute("SELECT version_name, count(*) FROM songs WHERE source = 'GT' GROUP BY version_name"):
    print(f"  {r[0]}: {r[1]:,}")

print("\nIndonesian songs count by source:")
for r in c.execute("SELECT source, count(*) FROM songs WHERE region = 'INDO' AND source != 'ChordTela' GROUP BY source"):
    print(f"  {r[0]}: {r[1]:,}")

indo_total = c.execute("SELECT count(*) FROM songs WHERE region = 'INDO' AND source != 'ChordTela'").fetchone()[0]
print(f"Total Indonesian songs: {indo_total:,}")

print("\nUG Versions breakdown:")
for r in c.execute("SELECT version_name, count(*) FROM songs WHERE source = 'UG' GROUP BY version_name ORDER BY count(*) DESC LIMIT 15"):
    print(f"  {r[0]}: {r[1]:,}")

print("\nUG Indonesian vs Non-Indonesian:")
ug_indo = c.execute("SELECT count(*) FROM songs WHERE source = 'UG' AND region = 'INDO'").fetchone()[0]
ug_non_indo = c.execute("SELECT count(*) FROM songs WHERE source = 'UG' AND region != 'INDO'").fetchone()[0]
print(f"  UG Indonesian: {ug_indo:,}")
print(f"  UG Non-Indonesian: {ug_non_indo:,}")

gt_indo = c.execute("SELECT count(*) FROM songs WHERE source = 'GT' AND region = 'INDO'").fetchone()[0]
gt_non_indo = c.execute("SELECT count(*) FROM songs WHERE source = 'GT' AND region != 'INDO'").fetchone()[0]
print(f"  GT Indonesian: {gt_indo:,}")
print(f"  GT Non-Indonesian: {gt_non_indo:,}")

conn.close()
