import csv

path = r'C:\KordApp-AGY\tools\catalog\curated_artists.csv'

curated = set()
with open(path, 'r', encoding='utf-8') as f:
    for r in csv.reader(f):
        if r:
            curated.add(r[0].strip().lower())
            curated.add("".join(c for c in r[0].strip().lower() if c.isalnum()))

indo_candidates = [
    "Jikustik", "The Rain", "Seurieus", "Ipang", "Caffeine",
    "Pongki Barata", "Astrid", "Audy", "Ello", "Vagetoz",
    "Dadali", "BIP", "Terry", "Papinka"
]

barat_candidates = [
    "Matchbox Twenty", "Dashboard Confessional", "Carpenters", "Lenny Kravitz",
    "Blondie", "Placebo", "Steely Dan", "Kylie Minogue", "Richard Marx",
    "Kesha", "Grateful Dead", "Mika", "Shinedown", "Linda Ronstadt",
    "Harry Nilsson", "Styx", "Misfits", "Lou Reed", "MxPx", "Luis Fonsi",
    "Noel Gallagher's High Flying Birds", "The Black Crowes", "Keith Urban",
    "Gordon Lightfoot", "Little Mix", "Tori Kelly", "Cat Power",
    "The Moody Blues", "38 Special", "Modest Mouse"
]

clean_indo = [a for a in indo_candidates if a.lower() not in curated and "".join(c for c in a.lower() if c.isalnum()) not in curated]
clean_barat = [a for a in barat_candidates if a.lower() not in curated and "".join(c for c in a.lower() if c.isalnum()) not in curated]

print("Indo to add:", len(clean_indo))
print(clean_indo)
print("\nBarat to add:", len(clean_barat))
print(clean_barat)
