import csv

curated = set()
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    for row in csv.reader(f):
        if row:
            curated.add(row[0].strip().lower())

western_candidates = [
    'the script', 'alter bridge', 'jimi hendrix', 'tears for fears', 'justin timberlake',
    'ellie goulding', 'dave matthews band', 'meghan trainor', 'status quo', 'tom jones',
    'the everly brothers', 'chris norman', 'morrissey', 'the hollies', 'manic street preachers',
    'cheap trick', 'steve earle', 'joan baez', 'john prine', 'kris kristofferson',
    'beck', 'ricky nelson', 'mcfly', 'jethro tull', 'donovan', 'biffy clyro', 'j.j. cale',
    'bonnie tyler', 'the monkees', 'the magnetic fields', 'jerry lee lewis', 'the byrds',
    'billy bragg', 't. rex', 'melissa etheridge', 'dropkick murphys', 'sheryl crow',
    'genesis', 'the national', 'carrie underwood', 'chuck berry', 'mayday parade',
    'reo speedwagon', 'johnny rivers', 'cliff richard', 'alice cooper', 'pulp',
    'aimee mann', 'kiss', 'take that', 'sleeping at last', 'bastille', 'hozier',
    'george ezra', 'kodaline', 'dean lewis', 'calum scott', 'james arthur', 'lewis capaldi',
    'sam smith', 'alec benjamin', 'lany', 'lauv', 'keshi', 'boy pablo', 'mac demarco',
    'rex orange county', 'phoebe bridgers', 'clairo', 'beabadoobee', 'mitski',
    'cigarettes after sex', 'men i trust', 'wallows', 'the 1975', 'foster the people',
    'two door cinema club', 'phoenix', 'passion pit', 'mgmt', 'vampire weekend',
    'arcade fire', 'the strokes', 'arctic monkeys', 'franz ferdinand', 'interpol',
    'bloc party', 'kasabian', 'the kooks', 'the libertines', 'razorlight', 'snow patrol',
    'keane', 'travis', 'coldplay', 'radiohead', 'muse', 'oasis', 'blur', 'pulp',
    'suede', 'supergrass', 'the verve', 'the stone roses', 'new order', 'the cure',
    'the smiths', 'depeche mode', 'joy division', 'echo & the bunnymen', 'u2',
    'simple minds', 'inxs', 'midnight oil', 'crowded house', 'men at work'
]

present = []
missing = []
for a in western_candidates:
    clean = a.lower()
    if clean in curated:
        present.append(a)
    else:
        missing.append(a)

print(f"Total Western checked: {len(western_candidates)}")
print(f"PRESENT ({len(present)}):")
print(", ".join(sorted(set(present))))

print(f"\nMISSING ({len(missing)}):")
print(", ".join(sorted(set(missing))))
