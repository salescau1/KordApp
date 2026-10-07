import glob
import csv
import re
import sys
from pathlib import Path
from bs4 import BeautifulSoup

# Ensure utf-8 output on Windows terminal
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

def main():
    existing = set()
    for f in glob.glob("tools/catalog/batches/batch_*.csv"):
        with open(f, encoding="utf-8") as fp:
            for row in csv.DictReader(fp):
                existing.add(row["artist"].strip().lower())

    print(f"Existing unique artists in CSV: {len(existing)}")

    def extract_from_file(path_str):
        p = Path(path_str)
        if not p.exists():
            return []
        soup = BeautifulSoup(p.read_text(encoding="utf-8", errors="ignore"), "html.parser")
        found = []
        for a in soup.select("a[href*='artist/']"):
            name = a.get_text().strip()
            if name and len(name) > 1 and name not in found:
                found.append(name)
        return found

    p_id = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2136\content.md"
    p_gl = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2140\content.md"
    p_ls = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2142\content.md"

    id_artists = extract_from_file(p_id)
    gl_artists = extract_from_file(p_gl)
    ls_artists = extract_from_file(p_ls)

    print(f"Extracted: ID Daily={len(id_artists)}, Global Daily={len(gl_artists)}, Listeners={len(ls_artists)}")

    # Keep track of where they came from
    artist_origins = {}
    for a in id_artists:
        if a.lower() not in artist_origins:
            artist_origins[a.lower()] = (a, "INDO")
    for a in gl_artists:
        if a.lower() not in artist_origins:
            artist_origins[a.lower()] = (a, "BARAT")
    for a in ls_artists:
        if a.lower() not in artist_origins:
            artist_origins[a.lower()] = (a, "BARAT")

    missing = []
    for lower_name, (orig_name, region) in artist_origins.items():
        if lower_name not in existing:
            missing.append((orig_name, region))

    print(f"Total unique missing Spotify artists: {len(missing)}")
    for i, (name, reg) in enumerate(missing, 1):
        print(f"{i:3d}. [{reg}] {name}")

if __name__ == "__main__":
    main()
