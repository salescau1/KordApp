import csv

curated = set()
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    for row in csv.reader(f):
        if row:
            curated.add(row[0].strip().lower())

test_artists = [
    'the script', 'alter bridge', 'jimi hendrix', 'tears for fears', 'justin timberlake',
    'beyonce', 'dave matthews band', 'ellie goulding', 'meghan trainor', 'last child',
    'naff', 'endank soekamti', 'shaggydog', 'steven & coconut treez', 'steven & coconuttreez',
    'souljah', 'danilla', 'vierratale', 'govinda', 'setia band', 'ten2five', 'vierra',
    'fiersa besari', 'hindia', 'sal priadi', 'bernardya', 'juicy luicy', 'pamungkas',
    'niki', 'rich brian', 'warren hue', 'barasuara', 'scaller', 'stars and rabbit',
    'reality club', 'mocca', 'white shoes & the couples company', 'the adams',
    'sore', 'efek rumah kaca', 'payung teduh', 'fourtwnty', 'morfem'
]

missing = []
present = []
for a in test_artists:
    if a in curated:
        present.append(a)
    else:
        missing.append(a)

print("PRESENT in curated:", len(present))
for a in sorted(present):
    print("  +", a)

print("\nMISSING from curated:", len(missing))
for a in sorted(missing):
    print("  -", a)
