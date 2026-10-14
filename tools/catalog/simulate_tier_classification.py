import sqlite3
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()

# Get all songs excluding ChordTela
c.execute("""
    SELECT id, source, version_name, votes, rating, region, artist, title 
    FROM songs 
    WHERE source != 'ChordTela'
""")
rows = c.fetchall()
total_songs = len(rows)
print(f"Total songs to classify: {total_songs}")

target_a = round(total_songs * 0.50) # 78684
target_b = round(total_songs * 0.30) # 47210
target_c = total_songs - target_a - target_b # 31474
print(f"Targets -> A (50%): {target_a}, B (30%): {target_b}, C (20%): {target_c}")

# Rule 1: All GT Easy Chords must go to A
gt_easy = [r for r in rows if r[1] == 'GT' and r[2] == 'Easy Chords']
# Rule 2: All GT Original Version must go to B
gt_orig = [r for r in rows if r[1] == 'GT' and r[2] == 'Original Version']

print(f"GT Easy Chords: {len(gt_easy)}")
print(f"GT Original: {len(gt_orig)}")

# Rule 3: Indonesian songs must NEVER be in C. They must be in A or B.
# UG Indonesian songs:
ug_indo = [r for r in rows if r[1] == 'UG' and r[5] == 'INDO']
print(f"UG Indonesian songs: {len(ug_indo)}")

# UG Non-Indonesian songs:
ug_non_indo = [r for r in rows if r[1] == 'UG' and r[5] != 'INDO']
print(f"UG Non-Indonesian songs: {len(ug_non_indo)}")

# Sort UG Non-Indo by popularity (votes desc, rating desc)
# High votes = Mainstream / Top hits
# Low votes / Ver 3+ = Non-mainstream
ug_non_indo.sort(key=lambda x: (x[3] or 0, x[4] or 0.0), reverse=True)

# UG Indo: sort by votes desc
ug_indo.sort(key=lambda x: (x[3] or 0, x[4] or 0.0), reverse=True)

tier_a_ids = set(r[0] for r in gt_easy)
tier_b_ids = set(r[0] for r in gt_orig)
tier_c_ids = set()

# Put top UG Indo into A, rest UG Indo into B
# How many slots left in A?
needed_in_a = target_a - len(tier_a_ids)
print(f"Slots left in Tier A: {needed_in_a}")

# Let's allocate top 6,000 UG Indo to A, and remaining ~4,253 UG Indo to B
ug_indo_for_a = ug_indo[:6000]
ug_indo_for_b = ug_indo[6000:]
for r in ug_indo_for_a:
    tier_a_ids.add(r[0])
for r in ug_indo_for_b:
    tier_b_ids.add(r[0])

# Now fill remaining Tier A from top UG Non-Indo
needed_in_a = target_a - len(tier_a_ids)
print(f"Slots left in Tier A after Indo: {needed_in_a}")
ug_non_indo_for_a = ug_non_indo[:needed_in_a]
for r in ug_non_indo_for_a:
    tier_a_ids.add(r[0])

remaining_ug_non_indo = ug_non_indo[needed_in_a:]
print(f"Remaining UG Non-Indo to distribute between B and C: {len(remaining_ug_non_indo)}")

# How many slots needed in B?
needed_in_b = target_b - len(tier_b_ids)
print(f"Slots needed in Tier B: {needed_in_b}")

# Fill B with the best remaining UG Non-Indo (higher rating / votes / studio tabs)
ug_non_indo_for_b = remaining_ug_non_indo[:needed_in_b]
for r in ug_non_indo_for_b:
    tier_b_ids.add(r[0])

# Everything else goes to C (strictly non-mainstream, low votes, obscure artists, deep cuts)
ug_non_indo_for_c = remaining_ug_non_indo[needed_in_b:]
for r in ug_non_indo_for_c:
    tier_c_ids.add(r[0])

print(f"\nFinal Distribution Results:")
print(f"Tier A: {len(tier_a_ids)} ({len(tier_a_ids)/total_songs*100:.2f}%) [Target: {target_a}]")
print(f"Tier B: {len(tier_b_ids)} ({len(tier_b_ids)/total_songs*100:.2f}%) [Target: {target_b}]")
print(f"Tier C: {len(tier_c_ids)} ({len(tier_c_ids)/total_songs*100:.2f}%) [Target: {target_c}]")
print(f"Total: {len(tier_a_ids) + len(tier_b_ids) + len(tier_c_ids)}")

# Sanity checks:
overlap = (tier_a_ids & tier_b_ids) | (tier_a_ids & tier_c_ids) | (tier_b_ids & tier_c_ids)
print(f"Overlap between tiers: {len(overlap)}")

# Indonesian check:
c_in_indo = [r for r in rows if r[0] in tier_c_ids and r[5] == 'INDO']
print(f"Indonesian songs in Tier C: {len(c_in_indo)} (MUST BE ZERO)")

# Check sample C songs (should be low votes, obscure / deep cuts):
sample_c = [r for r in rows if r[0] in tier_c_ids][:5]
print("\nSample Tier C songs (low votes / deep cuts):")
for s in sample_c:
    print(f"  [{s[1]}] {s[6]} - {s[7]} (votes={s[3]}, rating={s[4]}, ver={s[2]})")

# Check sample A songs:
sample_a = [r for r in rows if r[0] in tier_a_ids][:5]
print("\nSample Tier A songs:")
for s in sample_a:
    print(f"  [{s[1]}] {s[6]} - {s[7]} (votes={s[3]}, rating={s[4]}, ver={s[2]})")

# Check sample B songs:
sample_b = [r for r in rows if r[0] in tier_b_ids][:5]
print("\nSample Tier B songs:")
for s in sample_b:
    print(f"  [{s[1]}] {s[6]} - {s[7]} (votes={s[3]}, rating={s[4]}, ver={s[2]})")

conn.close()
