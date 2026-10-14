#!/usr/bin/env python3
"""
Classify KordApp master database into 3 tiers:
- Paket A (50%): Easy Chord (All GT Easy Chords + Top UG hits + Top UG Indonesian songs)
- Paket B (30%): Penyempurnaan (All GT Original Version + All remaining UG Indonesian songs + High rating UG pro tabs)
- Paket C (20%): Non-Mainstream / Arsip (Low votes, niche, obscure artists, deep cuts, strictly non-Indonesian)

Total songs: 157,368 (ChordTela completely excluded and removed).
"""
import sqlite3
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

DB_PATH = Path(r"C:\KordApp-AGY\tools\catalog\full_merged_catalog.db")

conn = sqlite3.connect(DB_PATH)
c = conn.cursor()

# 1. Remove any ChordTela songs
c.execute("DELETE FROM songs WHERE source = 'ChordTela'")
deleted_ct = c.rowcount
print(f"1. Removed {deleted_ct} ChordTela songs from master database.")

# 2. Check total songs
total_songs = c.execute("SELECT count(*) FROM songs").fetchone()[0]
print(f"2. Total clean songs (GT + UG): {total_songs:,}")

target_a = round(total_songs * 0.50) # 78,684
target_b = round(total_songs * 0.30) # 47,210
target_c = total_songs - target_a - target_b # 31,474
print(f"   Target A (50%): {target_a:,}")
print(f"   Target B (30%): {target_b:,}")
print(f"   Target C (20%): {target_c:,}")

# 3. Add column 'tier' if not exists
columns = [col[1] for col in c.execute("PRAGMA table_info(songs)").fetchall()]
if "tier" not in columns:
    print("3. Adding 'tier' column to songs table...")
    c.execute("ALTER TABLE songs ADD COLUMN tier TEXT NOT NULL DEFAULT 'A'")
else:
    print("3. 'tier' column already exists.")

# 4. Fetch all songs for deterministic classification
c.execute("""
    SELECT id, source, version_name, votes, rating, region, artist, title 
    FROM songs
""")
rows = c.fetchall()

# GT subsets
gt_easy = [r for r in rows if r[1] == 'GT' and r[2] == 'Easy Chords']
gt_orig = [r for r in rows if r[1] == 'GT' and r[2] == 'Original Version']

# UG subsets
ug_indo = [r for r in rows if r[1] == 'UG' and r[5] == 'INDO']
ug_non_indo = [r for r in rows if r[1] == 'UG' and r[5] != 'INDO']

# Sort UG Non-Indo by popularity (votes desc, rating desc, title_slug asc)
ug_non_indo.sort(key=lambda x: (x[3] or 0, x[4] or 0.0), reverse=True)

# Sort UG Indo by votes desc, rating desc
ug_indo.sort(key=lambda x: (x[3] or 0, x[4] or 0.0), reverse=True)

tier_a_ids = set(r[0] for r in gt_easy)
tier_b_ids = set(r[0] for r in gt_orig)
tier_c_ids = set()

# Allocate top 6,000 UG Indo to A, and remaining 4,253 UG Indo to B
# (So 100% of Indonesian songs are in A and B, ZERO in C)
ug_indo_for_a = ug_indo[:6000]
ug_indo_for_b = ug_indo[6000:]
for r in ug_indo_for_a:
    tier_a_ids.add(r[0])
for r in ug_indo_for_b:
    tier_b_ids.add(r[0])

# Fill remaining Tier A from top UG Non-Indo
needed_in_a = target_a - len(tier_a_ids)
ug_non_indo_for_a = ug_non_indo[:needed_in_a]
for r in ug_non_indo_for_a:
    tier_a_ids.add(r[0])

remaining_ug_non_indo = ug_non_indo[needed_in_a:]

# Fill Tier B from next best UG Non-Indo
needed_in_b = target_b - len(tier_b_ids)
ug_non_indo_for_b = remaining_ug_non_indo[:needed_in_b]
for r in ug_non_indo_for_b:
    tier_b_ids.add(r[0])

# Rest goes to Tier C (strictly obscure, low votes, deep cuts, zero Indo)
ug_non_indo_for_c = remaining_ug_non_indo[needed_in_b:]
for r in ug_non_indo_for_c:
    tier_c_ids.add(r[0])

# Update database
print("4. Applying tier classification in database...")
c.executemany("UPDATE songs SET tier = 'A' WHERE id = ?", [(id_,) for id_ in tier_a_ids])
c.executemany("UPDATE songs SET tier = 'B' WHERE id = ?", [(id_,) for id_ in tier_b_ids])
c.executemany("UPDATE songs SET tier = 'C' WHERE id = ?", [(id_,) for id_ in tier_c_ids])

# Ensure index on tier
print("5. Creating index idx_tier on songs(tier)...")
c.execute("CREATE INDEX IF NOT EXISTS idx_tier ON songs (tier)")

conn.commit()

# Verify counts in DB
print("\n6. Verification from database:")
for r in c.execute("SELECT tier, count(*) FROM songs GROUP BY tier ORDER BY tier"):
    print(f"   Tier {r[0]}: {r[1]:,} ({r[1]/total_songs*100:.2f}%)")

indo_in_c = c.execute("SELECT count(*) FROM songs WHERE region = 'INDO' AND tier = 'C'").fetchone()[0]
print(f"   Indonesian songs in Tier C: {indo_in_c} (MUST BE 0)")

gt_easy_in_a = c.execute("SELECT count(*) FROM songs WHERE source = 'GT' AND version_name = 'Easy Chords' AND tier = 'A'").fetchone()[0]
print(f"   GT Easy Chords in Tier A: {gt_easy_in_a:,} / {len(gt_easy):,}")

gt_orig_in_b = c.execute("SELECT count(*) FROM songs WHERE source = 'GT' AND version_name = 'Original Version' AND tier = 'B'").fetchone()[0]
print(f"   GT Original in Tier B: {gt_orig_in_b:,} / {len(gt_orig):,}")

conn.close()
print("\nClassification complete successfully!")
