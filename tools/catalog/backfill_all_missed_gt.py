import sqlite3
import sys
import time
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.fetcher import fetch_html
from crawler.parsers.guitartuna import parse_gt_html
from crawler.batch_crawler import load_gt_artist_index, discover_gt_songs

# Target list: (batch_num, artist_name, region, gt_url)
MISSED_ARTISTS = [
    (3, "Fade2Black", "INDO", "https://guitartuna.com/artists/fade2black-easy-original-guitar-chords-tabs-63b5a994cebb36caeb84e0b8"),
    (7, "The Wallflowers", "BARAT", "https://guitartuna.com/artists/the-wallflowers-easy-original-guitar-chords-tabs-616696c90251bdf6cf8e64d2"),
    (8, "Aretha Franklin", "BARAT", "https://guitartuna.com/artists/aretha-franklin-easy-original-guitar-chords-tabs-6374e616a60d78bb71b5c270"),
    (8, "D'Masiv", "INDO", "https://guitartuna.com/artists/dmasiv-easy-original-guitar-chords-tabs-66e29cad70183779376f0e09"),
    (8, "Deep Purple", "BARAT", "https://guitartuna.com/artists/deep-purple-easy-original-guitar-chords-tabs-6193ced3f71b2aa2f9c46bb4"),
    (8, "Iron Maiden", "BARAT", "https://guitartuna.com/artists/iron-maiden-easy-original-guitar-chords-tabs-6374f3e1a60d78bb71b5d000"),
    (8, "Sia", "BARAT", "https://guitartuna.com/artists/sia-easy-original-guitar-chords-tabs-6180023bb388c8ae4a51c9a3"),
    (9, "Utada Hikaru", "JAPAN", "https://guitartuna.com/artists/utada-hikaru-easy-original-guitar-chords-tabs-67647f9910d59e719d4201bc"),
    (9, "Wallows", "BARAT", "https://guitartuna.com/artists/wallows-easy-original-guitar-chords-tabs-6374fd2872dbfaef74894f2f"),
    (10, "Alter Bridge", "BARAT", "https://guitartuna.com/artists/alter-bridge-easy-original-guitar-chords-tabs-6374e616a60d78bb71b5c248"),
    (10, "Celine Dion", "BARAT", "https://guitartuna.com/artists/celine-dion-easy-original-guitar-chords-tabs-6763f793e1a2d2abbd2eb40e"),
    (10, "Goo Goo Dolls", "BARAT", "https://guitartuna.com/artists/goo-goo-dolls-easy-original-guitar-chords-tabs-61a296908140f1d62486c398"),
    (11, ".Feast", "INDO", "https://guitartuna.com/artists/feast-easy-original-guitar-chords-tabs-6764536610d59e719d41f58e"),
    (11, "Anggun", "INDO", "https://guitartuna.com/artists/anggun-easy-original-guitar-chords-tabs-66e1f00ff7abd9b95a896bd4"),
    (11, "Avenged Sevenfold", "BARAT", "https://guitartuna.com/artists/avenged-sevenfold-easy-original-guitar-chords-tabs-617f90d0d4cab0a7d65cfa5d"),
    (11, "Christina Aguilera", "BARAT", "https://guitartuna.com/artists/christina-aguilera-easy-original-guitar-chords-tabs-6374f3e1a60d78bb71b5ce09"),
    (11, "Fiona Apple", "BARAT", "https://guitartuna.com/artists/fiona-apple-easy-original-guitar-chords-tabs-6582b1b150408da384976d3c"),
    (11, "Ita Purnamasari", "INDO", "https://guitartuna.com/artists/ita-purnamasari-easy-original-guitar-chords-tabs-670911660a38030f85fc212b"),
    (11, "Matchbox Twenty", "BARAT", "https://guitartuna.com/artists/matchbox-twenty-easy-original-guitar-chords-tabs-61a01d0c8ad4f8fa4a670a14"),
    (11, "Radiohead", "BARAT", "https://guitartuna.com/artists/radiohead-easy-original-guitar-chords-tabs-616e901a7dabb7215237e339"),
    (12, "BLACKPINK", "KOREA", "https://guitartuna.com/artists/blackpink-easy-original-guitar-chords-tabs-644a66a5ec9d7d8edf4bcd56"),
    (12, "Dadali", "INDO", "https://guitartuna.com/artists/dadali-easy-original-guitar-chords-tabs-6a0acc1c6c6d82dd98ad2f6a"),
    (12, "Itzy", "KOREA", "https://guitartuna.com/artists/itzy-easy-original-guitar-chords-tabs-646f4ac9fc46cb745f6a98cb"),
    (12, "Luis Fonsi", "BARAT", "https://guitartuna.com/artists/luis-fonsi-easy-original-guitar-chords-tabs-63b4010a86cf0feaf42ed548"),
    (14, "Bangkutaman", "INDO", "https://guitartuna.com/artists/bangkutaman-easy-original-guitar-chords-tabs-67f3b634a43ff1c2b1e83965"),
    (14, "Ben E. King", "BARAT", "https://guitartuna.com/artists/ben-e-king-easy-original-guitar-chords-tabs-6374f3e1a60d78bb71b5cd1b"),
    (14, "Boys Like Girls", "BARAT", "https://guitartuna.com/artists/boys-like-girls-easy-original-guitar-chords-tabs-6374f3e1a60d78bb71b5cd7d"),
    (14, "Keshi", "BARAT", "https://guitartuna.com/artists/keshi-easy-original-guitar-chords-tabs-6374f3e1a60d78bb71b5d0d9"),
    (14, "Rossa", "INDO", "https://guitartuna.com/artists/rossa-easy-original-guitar-chords-tabs-66e20283f7abd9b95a896ed4"),
    (15, "BIP", "INDO", "https://guitartuna.com/artists/bip-easy-original-guitar-chords-tabs-69b76b033d745ddf33bcfb2e"),
    (15, "Blur", "BARAT", "https://guitartuna.com/artists/blur-easy-original-guitar-chords-tabs-617e7f48f1301c66588f4116"),
    (15, "Cokelat", "INDO", "https://guitartuna.com/artists/cokelat-easy-original-guitar-chords-tabs-6709340d0a38030f85fc26f9"),
]

def main():
    total_added = 0
    for batch_num, artist_name, region, gt_url in MISSED_ARTISTS:
        db_path = TOOLS_DIR / "catalog" / "db_batches" / f"batch_{batch_num:02d}.db"
        if not db_path.exists():
            continue

        conn = sqlite3.connect(str(db_path))
        c = conn.cursor()

        gt_targets = discover_gt_songs(artist_name, gt_url)
        print(f"\n[Batch {batch_num:02d}] {artist_name}: Discovered {len(gt_targets)} GT songs")

        inserted = 0
        for item in gt_targets:
            url = item["url"]
            try:
                c.execute("SELECT id FROM songs WHERE source_url = ?", (url,))
                if c.fetchone():
                    continue

                html_content = fetch_html(url)
                if not html_content:
                    continue

                parsed = parse_gt_html(html_content, url, artist_hint=artist_name, title_hint=item["title"])
                if parsed and parsed.content and parsed.chords:
                    c.execute("""
                        INSERT INTO songs (
                            source, source_url, title, artist, title_slug, artist_slug,
                            version_name, rating, votes, key_base, difficulty, category,
                            region, chords, chord_count, line_count, content, content_hash, scraped_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        parsed.source, parsed.source_url, parsed.title, parsed.artist,
                        parsed.title_slug, parsed.artist_slug, item["version_type"],
                        parsed.rating, parsed.votes, parsed.key_base, parsed.difficulty,
                        parsed.category, region, ",".join(parsed.chords),
                        parsed.chord_count, parsed.line_count, parsed.content,
                        parsed.content_hash, parsed.scraped_at
                    ))
                    conn.commit()
                    inserted += 1
                time.sleep(0.2)
            except Exception as e:
                pass

        conn.close()
        total_added += inserted
        print(f"  + Added {inserted} songs for {artist_name} (Total so far: {total_added})")

    print(f"\n=== BACKFILL COMPLETE: {total_added} GT songs added across all batches ===")

if __name__ == "__main__":
    main()
