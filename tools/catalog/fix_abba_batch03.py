import sqlite3
import sys
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

TOOLS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(TOOLS_DIR))

from crawler.fetcher import fetch_html
from crawler.parsers.guitartuna import parse_gt_html

def fix_abba():
    db_path = TOOLS_DIR / "catalog" / "db_batches" / "batch_03.db"
    conn = sqlite3.connect(str(db_path))
    c = conn.cursor()

    # 1. Remove the Russian/corrupt versions
    c.execute("DELETE FROM songs WHERE source_url = 'https://guitartuna.com/chords/dancing-queen-abba-easy-guitar-chords-68933ec4bdd22fba7dd62fca'")
    c.execute("DELETE FROM songs WHERE source_url = 'https://guitartuna.com/chords/dancing-queen-abba-abba-easy-guitar-chords-685c1d20562dd3f58b1adddc'")
    print(f"Deleted corrupt rows. Changes: {conn.total_changes}")

    # 2. Fetch and insert the TRUE matching Easy Chords (ID: 61af4676fa60da1313fc907e)
    true_easy_url = "https://guitartuna.com/chords/dancing-queen-abba-easy-guitar-chords-61af4676fa60da1313fc907e"
    html = fetch_html(true_easy_url)
    parsed = parse_gt_html(html, true_easy_url, artist_hint="ABBA", title_hint="Dancing Queen")

    if parsed and parsed.content and parsed.chords:
        c.execute("""
            INSERT OR REPLACE INTO songs (
                source, source_url, title, artist, title_slug, artist_slug,
                version_name, rating, votes, key_base, difficulty, category,
                region, chords, chord_count, line_count, content, content_hash, scraped_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            parsed.source, parsed.source_url, "Dancing Queen", "ABBA",
            "dancing-queen", "abba", "Easy Chords",
            parsed.rating, parsed.votes, parsed.key_base, parsed.difficulty,
            parsed.category, "BARAT", ",".join(parsed.chords),
            parsed.chord_count, parsed.line_count, parsed.content,
            parsed.content_hash, parsed.scraped_at
        ))
        conn.commit()
        print(f"Inserted TRUE Easy Chords version: {parsed.title} ({parsed.version_name}) chords={parsed.chords}")

    # 3. Verify final state in batch_03.db
    rows = c.execute("SELECT id, source, title, version_name, source_url FROM songs WHERE artist_slug = 'abba' AND title_slug = 'dancing-queen'").fetchall()
    print(f"\nFinal Dancing Queen rows in batch_03.db ({len(rows)} rows):")
    for r in rows:
        print(" ", r)

    conn.close()

if __name__ == "__main__":
    fix_abba()
