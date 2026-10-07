#!/usr/bin/env python3
"""
Build a SMALL, test-only catalog.db from the real GT and UG crawl databases.

HomeViewModelTest and other Robolectric catalog tests copy the shipped ~280 MB
assets/catalog.db per test, which is slow and flaky. This builds a schema-identical
tiny catalog (same `songs` table + three indices + source/content_hash conventions)
with only a few artists: enough for every hard-coded test expectation plus some
Indonesian artists for realism.

Sources (real crawl DBs, read-only):
  GT: C:\\ChordApp\\guitartuna_crawler\\guitartuna_merged.db   (source = "GT")
  UG: C:\\ChordApp\\ug_crawler\\data\\ug\\ug_catalog.db        (source = "UG")

Output: app/src/test/resources/catalog_test.db

Synthesised columns GT/UG lack: source ("GT"/"UG"), category (NULL),
content_hash (sha256(content) hex, verified to match production).

Run:  python tools/build_test_catalog.py
"""

import hashlib
import os
import sqlite3
import sys

GT_PATH = r"C:\ChordApp\guitartuna_crawler\guitartuna_merged.db"
UG_PATH = r"C:\ChordApp\ug_crawler\data\ug\ug_catalog.db"

OUT_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "app", "src", "test", "resources", "catalog_test.db"
)

REQUIRED_FOR_TESTS = ["ed-sheeran", "adele", "coldplay", "radiohead", "oasis"]
REQUESTED = ["adele", "muse", "queen", "sheila-on-7", "noah", "dewa-19", "raisa", "peterpan"]
ARTIST_SLUGS = sorted(set(REQUIRED_FOR_TESTS) | set(REQUESTED))

TARGET_SCHEMA = """
CREATE TABLE songs (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    source        TEXT NOT NULL,
    source_url    TEXT NOT NULL UNIQUE,
    title         TEXT NOT NULL,
    artist        TEXT NOT NULL,
    title_slug    TEXT NOT NULL,
    artist_slug   TEXT NOT NULL,
    version_name  TEXT,
    rating        REAL,
    votes         INTEGER,
    key_base      TEXT,
    difficulty    TEXT,
    category      TEXT,
    region        TEXT NOT NULL,
    chords        TEXT NOT NULL,
    chord_count   INTEGER NOT NULL,
    line_count    INTEGER NOT NULL,
    content       TEXT NOT NULL,
    content_hash  TEXT NOT NULL,
    scraped_at    TEXT NOT NULL
)
"""
INDICES = [
    "CREATE INDEX idx_source ON songs (source)",
    "CREATE INDEX idx_title_slug ON songs (title_slug)",
    "CREATE INDEX idx_artist_slug ON songs (artist_slug)",
]


def sha256(text):
    return hashlib.sha256((text or "").encode("utf-8")).hexdigest()


def fetch_gt(con, slug):
    cur = con.cursor()
    cur.execute(
        """SELECT source_url, title, artist, title_slug, artist_slug, version_name,
                  difficulty, region, chords, chord_count, line_count, content
           FROM songs WHERE artist_slug = ?""",
        (slug,),
    )
    out = []
    for (source_url, title, artist, title_slug, artist_slug, version_name,
         difficulty, region, chords, chord_count, line_count, content) in cur.fetchall():
        out.append({
            "source": "GT", "source_url": source_url, "title": title, "artist": artist,
            "title_slug": title_slug, "artist_slug": artist_slug, "version_name": version_name,
            "rating": None, "votes": None, "key_base": None, "difficulty": difficulty,
            "category": None, "region": region or "en", "chords": chords or "",
            "chord_count": chord_count or 0, "line_count": line_count or 0,
            "content": content or "", "content_hash": sha256(content),
            "scraped_at": "2026-09-15 00:00:00",
        })
    return out


def fetch_ug(con, slug):
    cur = con.cursor()
    cur.execute(
        """SELECT source_url, title, artist, title_slug, artist_slug, version_name,
                  rating, votes, key_base, difficulty, region, chords,
                  chord_count, line_count, content, scraped_at
           FROM songs WHERE artist_slug = ?""",
        (slug,),
    )
    out = []
    for (source_url, title, artist, title_slug, artist_slug, version_name,
         rating, votes, key_base, difficulty, region, chords,
         chord_count, line_count, content, scraped_at) in cur.fetchall():
        if not source_url:
            continue
        out.append({
            "source": "UG", "source_url": source_url, "title": title, "artist": artist,
            "title_slug": title_slug, "artist_slug": artist_slug, "version_name": version_name,
            "rating": rating, "votes": votes, "key_base": key_base or None,
            "difficulty": difficulty or None, "category": None, "region": region or "en",
            "chords": chords or "", "chord_count": chord_count or 0,
            "line_count": line_count or 0, "content": content or "",
            "content_hash": sha256(content), "scraped_at": scraped_at or "2026-09-19 00:00:00",
        })
    return out


def main():
    for path, label in [(GT_PATH, "GT"), (UG_PATH, "UG")]:
        if not os.path.exists(path):
            print("ERROR: %s source db not found: %s" % (label, path), file=sys.stderr)
            return 1

    gt = sqlite3.connect(GT_PATH)
    ug = sqlite3.connect(UG_PATH)
    collected = []
    for slug in ARTIST_SLUGS:
        collected.extend(fetch_gt(gt, slug))
        collected.extend(fetch_ug(ug, slug))
    gt.close()
    ug.close()

    seen = set()
    rows = []
    for r in collected:
        if r["source_url"] in seen:
            continue
        seen.add(r["source_url"])
        rows.append(r)

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    if os.path.exists(OUT_PATH):
        os.remove(OUT_PATH)

    out = sqlite3.connect(OUT_PATH)
    cur = out.cursor()
    cur.execute(TARGET_SCHEMA)
    for stmt in INDICES:
        cur.execute(stmt)

    cols = ["source", "source_url", "title", "artist", "title_slug", "artist_slug",
            "version_name", "rating", "votes", "key_base", "difficulty", "category",
            "region", "chords", "chord_count", "line_count", "content",
            "content_hash", "scraped_at"]
    placeholders = ",".join("?" for _ in cols)
    cur.executemany(
        "INSERT INTO songs (%s) VALUES (%s)" % (",".join(cols), placeholders),
        [tuple(r[c] for c in cols) for r in rows],
    )
    out.commit()

    cur.execute("SELECT COUNT(*) FROM songs")
    total = cur.fetchone()[0]
    cur.execute("SELECT source, COUNT(*) FROM songs GROUP BY source")
    by_source = cur.fetchall()
    cur.execute("SELECT COUNT(DISTINCT artist_slug) FROM songs")
    artists = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM (SELECT 1 FROM songs GROUP BY artist_slug, title_slug)")
    logical = cur.fetchone()[0]
    out.close()

    size = os.path.getsize(OUT_PATH)
    print("Wrote %s" % OUT_PATH)
    print("  rows=%d  by_source=%s  artists=%d  logical_songs=%d" % (
        total, dict(by_source), artists, logical))
    print("  size=%.1f KiB" % (size / 1024.0))
    return 0


if __name__ == "__main__":
    sys.exit(main())
