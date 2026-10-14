import sqlite3

conn = sqlite3.connect('tools/catalog/full_merged_catalog.db')
c = conn.cursor()

print("=== DISTRIBUSI 3 TIER MASTER DATABASE KORDAPP ===")
total = c.execute("SELECT count(*) FROM songs").fetchone()[0]
print(f"Total Lagu: {total:,}\n")

print("1. Distribusi Persentase & Jumlah Lagu:")
for tier, count in c.execute("SELECT tier, count(*) FROM songs GROUP BY tier ORDER BY tier"):
    pct = count / total * 100
    print(f"   Paket {tier}: {count:,} lagu ({pct:.2f}%)")

print("\n2. Sumber per Tier:")
for tier, src, count in c.execute("SELECT tier, source, count(*) FROM songs GROUP BY tier, source ORDER BY tier, source"):
    print(f"   Paket {tier} - {src}: {count:,} lagu")

print("\n3. Lagu Indonesia per Tier:")
for tier, count in c.execute("SELECT tier, count(*) FROM songs WHERE region='INDO' GROUP BY tier ORDER BY tier"):
    print(f"   Paket {tier}: {count:,} lagu Indonesia")

indo_c = c.execute("SELECT count(*) FROM songs WHERE region='INDO' AND tier='C'").fetchone()[0]
print(f"   Paket C: {indo_c} lagu Indonesia (0 = Sesuai Permintaan)")

print("\n4. GuitarTuna (GT) Versions:")
for tier, ver, count in c.execute("SELECT tier, version_name, count(*) FROM songs WHERE source='GT' GROUP BY tier, version_name ORDER BY tier"):
    print(f"   Paket {tier} - GT {ver}: {count:,} lagu")

conn.close()
