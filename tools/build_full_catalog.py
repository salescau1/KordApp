#!/usr/bin/env python3
"""
Build the FULL production catalog.db from the three real crawl DBs.

Reuses the exact schema + transform conventions of build_test_catalog.py
(same `songs` table + three indices + source/content_hash rules), but:
  - NO artist filter (all rows), and
  - adds the ChordTela source.

Sources (real crawl DBs, read-only):
  UG:        C:\\ChordApp\\ug_crawler\\data\\ug\\ug_catalog.db          (source = "UG")
  GT:        C:\\ChordApp\\guitartuna_crawler\\guitartuna_merged.db     (source = "GT")
  ChordTela: C:\\Chord APK\\Catalog\\chordtela.db                       (source = "ChordTela")

Rules (confirmed by owner):
  - Version selection already applied at crawl time -> take all rows as-is.
  - Dedup ONLY by source_url (cross-source dedup stays deferred).

Output: app/src/main/assets/catalog.db  (replaces the small placeholder)

Run:  python tools/build_full_catalog.py
"""

import hashlib
import os
import sqlite3
import sys

UG_PATH = r"C:\ChordApp\ug_crawler\data\ug\ug_catalog.db"
GT_PATH = r"C:\ChordApp\guitartuna_crawler\guitartuna_merged.db"
CT_PATH = r"C:\Chord APK\Catalog\chordtela.db"

OUT_PATH = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "app", "src", "main", "assets", "catalog.db",
)

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

COLS = ["source", "source_url", "title", "artist", "title_slug", "artist_slug",
        "version_name", "rating", "votes", "key_base", "difficulty", "category",
        "region", "chords", "chord_count", "line_count", "content",
        "content_hash", "scraped_at"]


def sha256(text):
    return hashlib.sha256((text or "").encode("utf-8")).hexdigest()


def rows_ug(con):
    cur = con.cursor()
    cur.execute(
        """SELECT source_url, title, artist, title_slug, artist_slug, version_name,
                  rating, votes, key_base, difficulty, region, chords,
                  chord_count, line_count, content, scraped_at
           FROM songs"""
    )
    for (source_url, title, artist, title_slug, artist_slug, version_name,
         rating, votes, key_base, difficulty, region, chords,
         chord_count, line_count, content, scraped_at) in cur:
        if not source_url:
            continue
        yield {
            "source": "UG", "source_url": source_url, "title": title, "artist": artist,
            "title_slug": title_slug, "artist_slug": artist_slug, "version_name": version_name,
            "rating": rating, "votes": votes, "key_base": key_base or None,
            "difficulty": difficulty or None, "category": None, "region": region or "en",
            "chords": chords or "", "chord_count": chord_count or 0,
            "line_count": line_count or 0, "content": content or "",
            "content_hash": sha256(content), "scraped_at": scraped_at or "2026-09-19 00:00:00",
        }


def rows_gt(con):
    cur = con.cursor()
    cur.execute(
        """SELECT source_url, title, artist, title_slug, artist_slug, version_name,
                  difficulty, region, chords, chord_count, line_count, content, scraped_at
           FROM songs"""
    )
    for (source_url, title, artist, title_slug, artist_slug, version_name,
         difficulty, region, chords, chord_count, line_count, content, scraped_at) in cur:
        if not source_url:
            continue
        yield {
            "source": "GT", "source_url": source_url, "title": title, "artist": artist,
            "title_slug": title_slug, "artist_slug": artist_slug, "version_name": version_name,
            "rating": None, "votes": None, "key_base": None, "difficulty": difficulty or None,
            "category": None, "region": region or "en", "chords": chords or "",
            "chord_count": chord_count or 0, "line_count": line_count or 0,
            "content": content or "", "content_hash": sha256(content),
            "scraped_at": scraped_at or "2026-09-15 00:00:00",
        }


def rows_ct(con):
    cur = con.cursor()
    cur.execute(
        """SELECT source_url, title, artist, title_slug, artist_slug, category,
                  region, chords, chord_count, line_count, content, scraped_at
           FROM songs"""
    )
    for (source_url, title, artist, title_slug, artist_slug, category,
         region, chords, chord_count, line_count, content, scraped_at) in cur:
        if not source_url:
            continue
        yield {
            "source": "ChordTela", "source_url": source_url, "title": title, "artist": artist,
            "title_slug": title_slug, "artist_slug": artist_slug, "version_name": None,
            "rating": None, "votes": None, "key_base": None, "difficulty": None,
            "category": category or None, "region": region or "en", "chords": chords or "",
            "chord_count": chord_count or 0, "line_count": line_count or 0,
            "content": content or "", "content_hash": sha256(content),
            "scraped_at": scraped_at or "2026-09-20 00:00:00",
        }


def main():
    for path, label in [(UG_PATH, "UG"), (GT_PATH, "GT"), (CT_PATH, "ChordTela")]:
        if not os.path.exists(path):
            print("ERROR: %s source db not found: %s" % (label, path), file=sys.stderr)
            return 1

    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    if os.path.exists(OUT_PATH):
        os.remove(OUT_PATH)

    out = sqlite3.connect(OUT_PATH)
    out.execute("PRAGMA journal_mode=OFF")
    out.execute("PRAGMA synchronous=OFF")
    cur = out.cursor()
    cur.execute(TARGET_SCHEMA)
    for stmt in INDICES:
        cur.execute(stmt)

    insert_sql = "INSERT OR IGNORE INTO songs (%s) VALUES (%s)" % (
        ",".join(COLS), ",".join("?" for _ in COLS))

    seen = set()
    per_source = {"UG": 0, "GT": 0, "ChordTela": 0}
    dupes = 0
    batch = []
    BATCH = 2000

    def flush():
        if batch:
            cur.executemany(insert_sql, batch)
            batch.clear()

    connectors = [
        ("UG", UG_PATH, rows_ug),
        ("GT", GT_PATH, rows_gt),
        ("ChordTela", CT_PATH, rows_ct),
    ]
    for label, path, gen in connectors:
        src = sqlite3.connect(path)
        for r in gen(src):
            u = r["source_url"]
            if u in seen:
                dupes += 1
                continue
            seen.add(u)
            per_source[label] += 1
            batch.append(tuple(r[c] for c in COLS))
            if len(batch) >= BATCH:
                flush()
        flush()
        src.close()
        print("  %-9s inserted=%d" % (label, per_source[label]))

    out.commit()
    cur.execute("SELECT COUNT(*) FROM songs")
    total = cur.fetchone()[0]
    cur.execute("SELECT source, COUNT(*) FROM songs GROUP BY source")
    by_source = dict(cur.fetchall())
    cur.execute("SELECT COUNT(DISTINCT artist_slug) FROM songs")
    artists = cur.fetchone()[0]
    out.close()

    size = os.path.getsize(OUT_PATH)
    print("Wrote %s" % OUT_PATH)
    print("  rows=%d  by_source=%s  artists=%d  dupes_skipped=%d" % (
        total, by_source, artists, dupes))
    print("  size=%.1f MB" % (size / 1048576.0))
    return 0


if __name__ == "__main__":
    sys.exit(main())
