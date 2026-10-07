import csv

curated = set()
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    for row in csv.reader(f):
        if row:
            curated.add(row[0].strip().lower())

bands = [
    'last child', 'naff', 'endank soekamti', 'shaggydog', 'souljah', 'danilla',
    'danilla riyadi', 'vierratale', 'govinda', 'setia band', 'ten2five', 'bernadya',
    'ungu', 'kotak', 'kahitna', 'gigi', 'padi', 'slank', "d'masiv", 'geisha',
    'repvblik', 'seventeen', 'armada', 'wali', 'kerispatih', 'yovie & nuno',
    'kangen band', 'radja', 'jamrud', 'boomerang', 'god bless', 'edane',
    'tipex', 'tipe-x', 'nidji', 'peterpan', 'samsons', 'j-rocks', 'the changcuters',
    'dewa 19', 'sheila on 7', 'padi reborn', 'letto', 'drive', 'ada band',
    'maliq & d\'essentials', 'ran', 'raisa', 'isyana sarasvati', 'afgan',
    'rossa', 'bunga citra lestari', 'glenn fredly', 'marcell', 'rio febrian',
    'judika', 'ari lasso', 'once mekel', 'anji', 'virgoun', 'cakra khan',
    'rizky febian', 'tiara andini', 'lyodra', 'ziva magnolya', 'mahalini',
    'keisya levronka', 'fabio asher', 'yura yunita', 'sal priadi', 'hindia',
    'fiersa besari', 'pamungkas', 'ardhito pramono', 'kunto aji', 'tulus',
    'judika', 'derby romero', 'vidi aldiano', 'gac', 'the overtunes',
    'hi-vi', 'hivi!', 'payung teduh', 'fourtwnty', 'barasuara', 'the adams',
    'efek rumah kaca', 'sore', 'white shoes & the couples company', 'mocca',
    'naif', 'kla project', 'chrisye', 'iwan fals', 'ebiet g ade', 'koes plus',
    'panbers', 'd\'lloyd', 'meriam bellina', 'nike ardilla', 'poppy mercury',
    'rhoma irama', 'mansyur s', 'meggy z', 'didik kempot', 'didi kempot',
    'denny caknan', 'guyon waton', 'happy asmara', 'ndx a.k.a', 'ndarboy genk'
]

present = []
missing = []
for b in bands:
    clean = b.lower()
    if clean in curated:
        present.append(b)
    else:
        missing.append(b)

print(f"Total checked: {len(bands)}")
print(f"PRESENT ({len(present)}):")
print(", ".join(sorted(present)))

print(f"\nMISSING ({len(missing)}):")
print(", ".join(sorted(missing)))
