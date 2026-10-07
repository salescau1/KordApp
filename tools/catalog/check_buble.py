import sqlite3
import glob
import csv

conn = sqlite3.connect("tools/catalog/full_merged_catalog.db")
c = conn.cursor()

c.execute("SELECT artist, artist_slug, title, title_slug, source_url FROM songs WHERE lower(artist) LIKE '%buble%' OR lower(artist_slug) LIKE '%buble%'")
rows = c.fetchall()
print(f"Total Michael Buble versions in full_merged_catalog.db: {len(rows)}")
for r in rows:
    print(" ", r)

print("\n--- Checking batches for Michael Buble ---")
for f in sorted(glob.glob("tools/catalog/batches/batch_*.csv")):
    with open(f, encoding="utf-8") as fp:
        for row in csv.DictReader(fp):
            if "buble" in row["artist"].lower():
                print(f"Found in {f}: {row}")
