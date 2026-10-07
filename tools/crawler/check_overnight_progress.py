import urllib.request
import json
import glob
import sqlite3
import os
import csv
import sys

sys.stdout.reconfigure(encoding='utf-8')

# 1. Total Curated Artists
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists_enriched.csv', 'r', encoding='utf-8') as f:
    curated_rows = list(csv.DictReader(f))
total_artists = len(curated_rows)
print(f"Total Curated Artists: {total_artists}")

# Breakdown by region
region_counts = {}
for r in curated_rows:
    reg = r.get('region', 'BARAT')
    region_counts[reg] = region_counts.get(reg, 0) + 1
for reg, cnt in region_counts.items():
    print(f"  - {reg}: {cnt} artis")

# 2. Check API status if server is running
print("\n--- API DASHBOARD STATUS ---")
try:
    res = urllib.request.urlopen('http://localhost:8000/api/status', timeout=5)
    status = json.loads(res.read().decode())
    print("API Status: ONLINE")
    print(f"Running Batch: {status.get('running_batch')}")
    print(f"Current Artist: {status.get('current_artist')} [{status.get('current_artist_index')}/{status.get('current_artist_total')}]")
    print(f"Total Songs All: {status.get('total_songs_all')}")
except Exception as e:
    print("API Offline / Unreachable:", e)

# 3. Check SQLite DB Files on Disk
print("\n--- DATABASE BATCHES ON DISK ---")
db_files = sorted(glob.glob(r'C:\KordApp-AGY\tools\catalog\db_batches\batch_*.db'))
total_songs = 0
for db in db_files:
    b_name = os.path.basename(db)
    con = sqlite3.connect(db)
    c = con.cursor()
    cnt = c.execute("SELECT count(*) FROM songs").fetchone()[0]
    total_songs += cnt
    sources = c.execute("SELECT source, count(*) FROM songs GROUP BY source").fetchall()
    artists_in_db = c.execute("SELECT count(DISTINCT artist) FROM songs").fetchone()[0]
    con.close()
    src_str = ", ".join(f"{s[0]}: {s[1]}" for s in sources)
    print(f"  {b_name}: {cnt} lagu ({artists_in_db} artis) | [{src_str}]")

print(f"\nTotal Songs Scraped So Far: {total_songs} lagu")

# 4. Check recent logs from API
print("\n--- RECENT LOGS ---")
try:
    res_logs = urllib.request.urlopen('http://localhost:8000/api/logs', timeout=5)
    logs = json.loads(res_logs.read().decode())['logs']
    for l in logs[-15:]:
        print(" ", l)
except Exception as e:
    print("Could not fetch logs:", e)
