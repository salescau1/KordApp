import sqlite3
import csv
import sys

sys.stdout.reconfigure(encoding='utf-8')

# Check UG catalog DB (which has 1225 artists, 100k+ chords) and GT merged DB (which has 1127 artists, 70k+ chords)
con_ug = sqlite3.connect(r'C:\ChordApp\ug_crawler\data\ug\ug_catalog.db')
con_gt = sqlite3.connect(r'C:\ChordApp\guitartuna_crawler\guitartuna_merged.db')

# Let's inspect Indonesian artists in UG catalog
ug_indo = []
for row in con_ug.execute("SELECT artist, count(*) FROM songs GROUP BY artist"):
    art, cnt = row[0], row[1]
    ug_indo.append((art, cnt))

print(f"Total artists in UG db: {len(ug_indo)}")

# Let's inspect Indonesian artists in GT db
gt_indo = []
for row in con_gt.execute("SELECT artist, count(*) FROM songs GROUP BY artist"):
    art, cnt = row[0], row[1]
    gt_indo.append((art, cnt))

print(f"Total artists in GT db: {len(gt_indo)}")
