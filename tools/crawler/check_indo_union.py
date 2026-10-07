import csv
import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8')

curated = set()
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    for row in csv.reader(f):
        if row:
            curated.add(row[0].strip().lower())

# Let's inspect all Indonesian artists in artist_union.csv
indo_union = []
with open(r'C:\ChordApp\ug_crawler\data\ug\artist_union.csv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f):
        if row.get('chart_region') == 'INDO' or row.get('country') in ('ID', 'Indonesia'):
            art = row['artist'].strip()
            if art.lower() not in curated:
                indo_union.append(row)

print(f"Indo artists in artist_union missing from curated: {len(indo_union)}")
print("\nList of missing Indo artists from artist_union:")
for row in indo_union:
    print(f"  {row['artist']:<30} | {row.get('genre','')}")
