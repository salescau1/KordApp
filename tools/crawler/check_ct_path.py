import sqlite3, os

UG_PATH = r"C:\ChordApp\ug_crawler\data\ug\ug_catalog.db"
GT_PATH = r"C:\ChordApp\guitartuna_crawler\guitartuna_merged.db"
CT_PATH = r"C:\Chord APK\Catalog\chordtela.db"

# CT path in generate_artist_list.py was "C:\Chord APK\Catalog\chordtela.db" which might not exist!
print("C:\\Chord APK\\Catalog\\chordtela.db exists?", os.path.exists(CT_PATH))
print("C:\\ChordApp\\chordtela.db exists?", os.path.exists(r"C:\ChordApp\chordtela.db"))
print("C:\\KordApp-AGY\\Catalog\\chordtela.db exists?", os.path.exists(r"C:\KordApp-AGY\Catalog\chordtela.db"))
