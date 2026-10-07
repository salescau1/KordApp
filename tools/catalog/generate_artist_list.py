import os, csv, re, sqlite3

UG_PATH = r"C:\ChordApp\ug_crawler\data\ug\ug_catalog.db"
GT_PATH = r"C:\ChordApp\guitartuna_crawler\guitartuna_merged.db"
CT_PATH = r"C:\Chord APK\Catalog\chordtela.db"
OUT_CSV = r"C:\KordApp-AGY\tools\catalog\curated_artists.csv"

EXCLUDED_ARTISTS = {
    "misc cartoons",
    "misc soundtrack",
    "various artists",
    "soundtrack",
}

KNOWN_JAPAN = {
    "radwimps", "one ok rock", "yoasobi", "kenshi yonezu", "lisa", "ado",
    "aimer", "vaundy", "fujii kaze", "eve", "asian kung-fu generation",
    "official hige dandism", "king gnu", "milet", "spyair", "kana-boon",
    "scandal", "babymetal", "flow", "tk from ling tosite sigure", "yui",
    "joe hisaishi", "utada hikaru", "sheena ringo", "l'arc~en~ciel", "arashi",
    "aqua timez", "an cafe", "miyavi", "gazette", "the gazette"
}

KNOWN_KOREA = {
    "bts", "blackpink", "twice", "iu", "newjeans", "stray kids", "exo",
    "red velvet", "seventeen", "nct", "aespa", "le sserafim", "ive",
    "txt", "enhypen", "itzy", "day6", "akmu", "taeyeon", "shinee",
    "bigbang", "girls' generation", "super junior", "psy", "got7",
    "monsta x", "ikon", "mamamoo", "ateez", "treasure"
}

NON_INDO_EXCEPTIONS = {
    "luis fonsi", "air", "monica", "asian kung-fu generation", "got7"
}

def is_japanese_script(text: str) -> bool:
    return bool(re.search(r'[\u3040-\u30ff\u4e00-\u9fff]', text))

def is_korean_script(text: str) -> bool:
    return bool(re.search(r'[\uac00-\ud7af\u1100-\u11ff]', text))

def choose_better_casing(name1: str, name2: str) -> str:
    if name1.isupper() and not name2.isupper():
        return name2
    if name2.isupper() and not name1.isupper():
        return name1
    if name1.islower() and not name2.islower():
        return name2
    if name2.islower() and not name1.islower():
        return name1
    return name1

# 1. Build Indonesian reference set from CT and GT
con_ct = sqlite3.connect(CT_PATH)
ct_cur = con_ct.cursor()
ct_indo = set(r[0].strip().lower() for r in ct_cur.execute("""
    SELECT artist
    FROM songs
    GROUP BY lower(trim(artist))
    HAVING SUM(CASE WHEN region = 'id' THEN 1 ELSE 0 END) > SUM(CASE WHEN region = 'en' THEN 1 ELSE 0 END)
""").fetchall() if r[0])
con_ct.close()

con_gt = sqlite3.connect(GT_PATH)
gt_indo = set(r[0].strip().lower() for r in con_gt.execute("SELECT DISTINCT artist FROM crawl_queue WHERE region = 'id'").fetchall() if r[0])
con_gt.close()

all_indo_ref = (ct_indo | gt_indo) - NON_INDO_EXCEPTIONS

# 2. Gather unique artists from UG and GT
artist_map = {}
for path in [GT_PATH, UG_PATH]:
    if not os.path.exists(path):
        continue
    con = sqlite3.connect(path)
    for r in con.execute("SELECT DISTINCT artist FROM songs"):
        raw_name = (r[0] or "").strip()
        if not raw_name or raw_name.lower() in EXCLUDED_ARTISTS:
            continue
        lower_name = raw_name.lower()
        if lower_name in artist_map:
            artist_map[lower_name] = choose_better_casing(artist_map[lower_name], raw_name)
        else:
            artist_map[lower_name] = raw_name
    con.close()

# 3. Classify
rows = []
for lower_name, clean_name in artist_map.items():
    if is_japanese_script(clean_name) or lower_name in KNOWN_JAPAN:
        region = "JAPAN"
    elif is_korean_script(clean_name) or lower_name in KNOWN_KOREA:
        region = "KOREA"
    elif lower_name in all_indo_ref:
        region = "INDO"
    else:
        region = "BARAT"

    rows.append({
        "artist_name": clean_name,
        "category_region": region,
        "is_active": "1"
    })

# 4. Sort: INDO -> BARAT -> JAPAN -> KOREA, then A-Z
region_priority = {"INDO": 1, "BARAT": 2, "JAPAN": 3, "KOREA": 4}
rows.sort(key=lambda r: (region_priority.get(r["category_region"], 99), r["artist_name"].lower()))

# Write
with open(OUT_CSV, "w", encoding="utf-8", newline="") as f:
    writer = csv.DictWriter(f, fieldnames=["artist_name", "category_region", "is_active"])
    writer.writeheader()
    writer.writerows(rows)

print("Updated curated_artists.csv successfully!")
counts = {}
for r in rows:
    counts[r["category_region"]] = counts.get(r["category_region"], 0) + 1
for reg, cnt in counts.items():
    print(f"  - {reg}: {cnt} artists")
