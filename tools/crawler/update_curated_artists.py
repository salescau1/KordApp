import csv

CURATED_PATH = r'C:\KordApp-AGY\tools\catalog\curated_artists.csv'

# Read existing
existing = []
existing_lookup = set()
with open(CURATED_PATH, 'r', encoding='utf-8') as f:
    r = csv.reader(f)
    header = next(r)
    for row in r:
        if row:
            existing.append({
                "artist_name": row[0].strip(),
                "category_region": row[1].strip() if len(row) > 1 else "BARAT",
                "is_active": row[2].strip() if len(row) > 2 else "1"
            })
            existing_lookup.add(row[0].strip().lower())
            # also alphanumeric
            existing_lookup.add("".join(c for c in row[0].strip().lower() if c.isalnum()))

def is_present(name):
    norm = name.strip().lower()
    if norm in existing_lookup:
        return True
    return "".join(c for c in norm if c.isalnum()) in existing_lookup

# New Indo artists to add
indo_additions = [
    "Burgerkill", "D'Lloyd", "Danilla", "Derby Romero", "Diskoria",
    "Eleventwelfth", "Endank Soekamti", "Fabio Asher", "Fariz RM", "GAC",
    "Gangga", "Goodnight Electric", "Govinda", "Gugun Blues Shelter",
    "Keisya Levronka", "Last Child", "Meriam Bellina", "Morfem", "Naff",
    "NDX A.K.A", "Nike Ardilla", "Poppy Mercury", "Rio Febrian", "Sajama Cut",
    "Scaller", "Setia Band", "Shaggydog", "Souljah", "Stars and Rabbit",
    "Ten2Five", "The Overtunes", "The S.I.G.I.T.", "Tigapagi", "Vierratale",
    "White Shoes & The Couples Company"
]

# New Barat artists to add
barat_additions = [
    "Alice Cooper", "Alter Bridge", "Arcade Fire", "Beck", "Biffy Clyro",
    "Bonnie Tyler", "Boy Pablo", "Carrie Underwood", "Cheap Trick", "Chris Norman",
    "Chuck Berry", "Cliff Richard", "Crowded House", "Dave Matthews Band", "Dean Lewis",
    "Donovan", "Dropkick Murphys", "Echo & The Bunnymen", "Ellie Goulding", "Franz Ferdinand",
    "Genesis", "INXS", "Interpol", "J.J. Cale", "Jethro Tull", "Jimi Hendrix",
    "Joan Baez", "John Prine", "Joy Division", "Justin Timberlake", "Kasabian",
    "Keshi", "KISS", "Mac DeMarco", "Manic Street Preachers", "Mayday Parade",
    "McFly", "Meghan Trainor", "Men At Work", "Men I Trust", "MGMT",
    "Midnight Oil", "Morrissey", "New Order", "Phoenix", "Pulp",
    "REO Speedwagon", "Rex Orange County", "Sheryl Crow", "Simple Minds",
    "Sleeping at Last", "Status Quo", "Steve Earle", "Supergrass", "Take That",
    "Tears For Fears", "The Byrds", "The Everly Brothers", "The Hollies", "The Libertines",
    "The Monkees", "The National", "The Script", "The Stone Roses", "The Verve",
    "Tom Jones", "Two Door Cinema Club", "Vampire Weekend", "Wallows"
]

added_indo = 0
for name in indo_additions:
    if not is_present(name):
        existing.append({
            "artist_name": name,
            "category_region": "INDO",
            "is_active": "1"
        })
        existing_lookup.add(name.lower())
        existing_lookup.add("".join(c for c in name.lower() if c.isalnum()))
        added_indo += 1

added_barat = 0
for name in barat_additions:
    if not is_present(name):
        existing.append({
            "artist_name": name,
            "category_region": "BARAT",
            "is_active": "1"
        })
        existing_lookup.add(name.lower())
        existing_lookup.add("".join(c for c in name.lower() if c.isalnum()))
        added_barat += 1

# Sort cleanly: INDO -> BARAT -> JAPAN -> KOREA, then A-Z
region_order = {"INDO": 1, "BARAT": 2, "JAPAN": 3, "KOREA": 4}
existing.sort(key=lambda x: (region_order.get(x["category_region"], 99), x["artist_name"].lower()))

# Write back
with open(CURATED_PATH, 'w', encoding='utf-8', newline='') as f:
    w = csv.DictWriter(f, fieldnames=["artist_name", "category_region", "is_active"])
    w.writeheader()
    w.writerows(existing)

print(f"Successfully added {added_indo} INDO artists and {added_barat} BARAT artists.")
print(f"Total artists now in curated_artists.csv: {len(existing)}")

counts = {}
for x in existing:
    counts[x["category_region"]] = counts.get(x["category_region"], 0) + 1
for reg, cnt in counts.items():
    print(f"  - {reg}: {cnt} artists")
