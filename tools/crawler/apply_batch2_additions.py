import csv

path = r'C:\KordApp-AGY\tools\catalog\curated_artists.csv'

rows = []
lookup = set()
with open(path, 'r', encoding='utf-8') as f:
    r = csv.reader(f)
    header = next(r)
    for row in r:
        if row:
            rows.append({
                'artist_name': row[0].strip(),
                'category_region': row[1].strip(),
                'is_active': row[2].strip()
            })
            lookup.add(row[0].strip().lower())
            lookup.add("".join(c for c in row[0].strip().lower() if c.isalnum()))

clean_indo = [
    "Jikustik", "The Rain", "Seurieus", "Ipang", "Caffeine",
    "Pongki Barata", "Astrid", "Audy", "Ello", "Vagetoz",
    "Dadali", "BIP", "Terry", "Papinka"
]

clean_barat = [
    "Matchbox Twenty", "Dashboard Confessional", "Carpenters", "Lenny Kravitz",
    "Blondie", "Placebo", "Steely Dan", "Kylie Minogue", "Richard Marx",
    "Kesha", "Grateful Dead", "Mika", "Shinedown", "Linda Ronstadt",
    "Harry Nilsson", "Styx", "Misfits", "Lou Reed", "MxPx", "Luis Fonsi",
    "Noel Gallagher's High Flying Birds", "The Black Crowes", "Keith Urban",
    "Gordon Lightfoot", "Little Mix", "Tori Kelly", "Cat Power",
    "The Moody Blues", "38 Special", "Modest Mouse"
]

added_indo = 0
for name in clean_indo:
    if name.lower() not in lookup and "".join(c for c in name.lower() if c.isalnum()) not in lookup:
        rows.append({'artist_name': name, 'category_region': 'INDO', 'is_active': '1'})
        lookup.add(name.lower())
        lookup.add("".join(c for c in name.lower() if c.isalnum()))
        added_indo += 1

added_barat = 0
for name in clean_barat:
    if name.lower() not in lookup and "".join(c for c in name.lower() if c.isalnum()) not in lookup:
        rows.append({'artist_name': name, 'category_region': 'BARAT', 'is_active': '1'})
        lookup.add(name.lower())
        lookup.add("".join(c for c in name.lower() if c.isalnum()))
        added_barat += 1

region_order = {'INDO': 1, 'BARAT': 2, 'JAPAN': 3, 'KOREA': 4}
rows.sort(key=lambda x: (region_order.get(x['category_region'], 99), x['artist_name'].lower()))

with open(path, 'w', encoding='utf-8', newline='') as f:
    w = csv.DictWriter(f, fieldnames=['artist_name', 'category_region', 'is_active'])
    w.writeheader()
    w.writerows(rows)

print(f"Successfully added {added_indo} INDO and {added_barat} BARAT artists.")
print(f"New total in curated_artists.csv: {len(rows)}")

counts = {}
for x in rows:
    counts[x['category_region']] = counts.get(x['category_region'], 0) + 1
for reg, cnt in counts.items():
    print(f"  - {reg}: {cnt} artists")
