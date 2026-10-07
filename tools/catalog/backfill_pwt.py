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

def backfill_artist_gt(artist_name: str, region: str, batch_num: int):
    gt_index = load_gt_artist_index()
    gt_url = gt_index.get(artist_name.lower().strip())
    if not gt_url:
        print(f"[{artist_name}] No URL in GT index!")
        return 0

    db_path = TOOLS_DIR / "catalog" / "db_batches" / f"batch_{batch_num:02d}.db"
    conn = sqlite3.connect(str(db_path))
    c = conn.cursor()

    gt_targets = discover_gt_songs(artist_name, gt_url)
    print(f"[{artist_name}] Discovered {len(gt_targets)} GT songs from {gt_url}")

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
                print(f"  + [GT OK] {parsed.artist} - {parsed.title} ({item['version_type']})")
            time.sleep(0.2)
        except Exception as e:
            print(f"  ! [GT ERR] {url}: {e}")

    conn.close()
    print(f"[{artist_name}] Total inserted: {inserted} songs into batch_{batch_num:02d}.db")
    return inserted

if __name__ == "__main__":
    backfill_artist_gt("Plain White T's", "BARAT", 8)
