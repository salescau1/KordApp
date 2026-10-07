import csv

curated = {}
with open(r'C:\KordApp-AGY\tools\catalog\curated_artists.csv', 'r', encoding='utf-8') as f:
    for r in csv.reader(f):
        if r:
            curated[r[0].strip().lower()] = (r[0].strip(), r[1].strip() if len(r)>1 else 'BARAT')

def is_curated(name):
    norm = name.strip().lower()
    if norm in curated:
        return True
    can = "".join(c for c in norm if c.isalnum())
    for k in curated:
        if "".join(c for c in k if c.isalnum()) == can:
            return True
    return False

# Top Indo Artists that are verified missing:
indo_to_add = [
    # Top Pop / Rock / Emo Bands
    "Last Child", "Naff", "Endank Soekamti", "Shaggydog", "Souljah",
    "Danilla", "Vierratale", "Govinda", "Setia Band", "Ten2Five",
    # Pop / Ballad / Nostalgia
    "Nike Ardilla", "Poppy Mercury", "Rio Febrian", "D'Lloyd",
    "Meriam Bellina", "Derby Romero", "GAC", "The Overtunes",
    # Modern / Pop Hits
    "Fabio Asher", "Keisya Levronka", "NDX A.K.A", "Gangga", "Diskoria",
    # Indie / Alternative / Rock
    "White Shoes & The Couples Company", "Stars and Rabbit", "Morfem", "Scaller",
    "The S.I.G.I.T.", "Burgerkill", "Goodnight Electric", "Gugun Blues Shelter",
    "Eleventwelfth", "Sajama Cut", "Fariz RM", "Tigapagi"
]

# Top Barat Artists that are verified missing:
barat_to_add = [
    # Pop / Modern Top Artists
    "The Script", "Justin Timberlake", "Ellie Goulding", "Meghan Trainor", "Dean Lewis",
    "Keshi", "Boy Pablo", "Rex Orange County", "Men I Trust", "Wallows", "Sleeping at Last",
    # Rock / Alt-Rock / Post-Grunge / Metal
    "Alter Bridge", "Jimi Hendrix", "KISS", "Alice Cooper", "Status Quo",
    "Cheap Trick", "Mayday Parade", "Biffy Clyro", "Dropkick Murphys", "Manic Street Preachers",
    # Classic Rock / 80s / 70s / Pop-Rock Legends
    "Tears For Fears", "The Verve", "Genesis", "Chuck Berry", "The Everly Brothers",
    "Tom Jones", "Bonnie Tyler", "Chris Norman", "The Hollies", "The Monkees",
    "The Byrds", "T. Rex", "Cliff Richard", "REO Speedwagon", "Dave Matthews Band",
    # Indie Rock / Post-Punk / Britpop
    "New Order", "Joy Division", "Franz Ferdinand", "Interpol", "Two Door Cinema Club",
    "Vampire Weekend", "MGMT", "Mac DeMarco", "The Stone Roses", "Supergrass",
    "The Libertines", "Kasabian", "The National", "Arcade Fire", "Phoenix",
    "Morrissey", "Beck", "Pulp", "Echo & The Bunnymen",
    # Singer-Songwriter / Folk / Country
    "Sheryl Crow", "Carrie Underwood", "John Prine", "Steve Earle", "Joan Baez",
    "Donovan", "J.J. Cale", "Jethro Tull", "McFly", "Take That",
    "Crowded House", "Men At Work", "INXS", "Midnight Oil", "Simple Minds"
]

clean_indo = []
for a in indo_to_add:
    if not is_curated(a):
        clean_indo.append(a)

clean_barat = []
for a in barat_to_add:
    if not is_curated(a):
        clean_barat.append(a)

print(f"Indo to add: {len(clean_indo)}")
print(clean_indo)
print(f"\nBarat to add: {len(clean_barat)}")
print(clean_barat)
