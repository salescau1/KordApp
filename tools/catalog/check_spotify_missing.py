import glob
import csv
import sys
from pathlib import Path
from bs4 import BeautifulSoup

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

def main():
    existing = set()
    for f in glob.glob("tools/catalog/batches/batch_*.csv"):
        with open(f, encoding="utf-8") as fp:
            for row in csv.DictReader(fp):
                existing.add(row["artist"].strip().lower())

    p_id = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2136\content.md"
    soup = BeautifulSoup(Path(p_id).read_text(encoding="utf-8", errors="ignore"), "html.parser")

    rows = []
    for tr in soup.find_all("tr"):
        td_title = tr.find("td", class_="text mp")
        if not td_title:
            continue
        art_tags = td_title.find_all("a", href=lambda h: h and "artist" in h)
        trk_tag = td_title.find("a", href=lambda h: h and "track" in h)
        if art_tags and trk_tag:
            artists = [a.get_text().strip() for a in art_tags]
            track = trk_tag.get_text().strip()
            rows.append((artists, track))

    print(f"Parsed {len(rows)} entries from Spotify Indonesia Daily")
    missing_indo = []
    seen = set()
    for artists, track in rows:
        for a in artists:
            if a.lower() not in existing and a.lower() not in seen:
                seen.add(a.lower())
                missing_indo.append((a, track))

    print(f"\nMissing artists from Spotify Indonesia ({len(missing_indo)}):")
    for a, trk in missing_indo:
        print(f"  - {a} (Top track: {trk})")

    # Now let's do Global Top
    p_gl = r"C:\Users\yousu\.gemini\antigravity-cli\brain\6266fdd4-676b-4d3a-ad06-b54a8aa5ab8e\.system_generated\steps\2140\content.md"
    soup_gl = BeautifulSoup(Path(p_gl).read_text(encoding="utf-8", errors="ignore"), "html.parser")
    rows_gl = []
    for tr in soup_gl.find_all("tr"):
        td_title = tr.find("td", class_="text mp")
        if not td_title:
            continue
        art_tags = td_title.find_all("a", href=lambda h: h and "artist" in h)
        trk_tag = td_title.find("a", href=lambda h: h and "track" in h)
        if art_tags and trk_tag:
            artists = [a.get_text().strip() for a in art_tags]
            track = trk_tag.get_text().strip()
            rows_gl.append((artists, track))

    missing_gl = []
    for artists, track in rows_gl:
        for a in artists:
            if a.lower() not in existing and a.lower() not in seen:
                seen.add(a.lower())
                missing_gl.append((a, track))

    print(f"\nMissing artists from Spotify Global ({len(missing_gl)}):")
    for a, trk in missing_gl[:50]:
        print(f"  - {a} (Top track: {trk})")

if __name__ == "__main__":
    main()
