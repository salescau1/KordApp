import sys
sys.path.insert(0, r'C:\KordApp-AGY\tools')
from crawler.parsers.ultimate_guitar import UltimateGuitarParser
from crawler.fetcher import SmartFetcher

sys.stdout.reconfigure(encoding='utf-8')

test_names = [
    "The Script", "Alter Bridge", "Jimi Hendrix", "Tears For Fears", "Justin Timberlake",
    "Ellie Goulding", "Last Child", "Naff", "Endank Soekamti", "Shaggydog",
    "Souljah", "Danilla", "Vierratale", "Govinda",
    "Setia Band", "Ten2Five", "Bernadya"
]

ug_parser = UltimateGuitarParser()
fetcher = SmartFetcher()

for name in test_names:
    results = ug_parser.discover_artist_songs(name, fetcher)
    print(f"{name:<25} -> {len(results)} songs discovered on UG")
