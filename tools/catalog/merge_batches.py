#!/usr/bin/env python3
"""
Merges individual batch SQLite databases (batch_01.db .. batch_15.db)
into a single consolidated catalog database, and compresses to catalog.db.xz.
"""
import glob
import lzma
import os
import sqlite3
import sys
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent.parent
DB_BATCHES_DIR = TOOLS_DIR / "catalog" / "db_batches"
MERGED_DB = TOOLS_DIR / "catalog" / "full_merged_catalog.db"
ASSET_XZ = TOOLS_DIR.parent / "app" / "src" / "main" / "assets" / "catalog.db.xz"

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS `songs` (
    `id` INTEGER,
    `source` TEXT NOT NULL,
    `source_url` TEXT NOT NULL,
    `title` TEXT NOT NULL,
    `artist` TEXT NOT NULL,
    `title_slug` TEXT NOT NULL,
    `artist_slug` TEXT NOT NULL,
    `version_name` TEXT,
    `rating` REAL,
    `votes` INTEGER,
    `key_base` TEXT,
    `difficulty` TEXT,
    `category` TEXT,
    `region` TEXT NOT NULL,
    `chords` TEXT NOT NULL,
    `chord_count` INTEGER NOT NULL,
    `line_count` INTEGER NOT NULL,
    `content` TEXT NOT NULL,
    `content_hash` TEXT NOT NULL,
    `scraped_at` TEXT NOT NULL,
    PRIMARY KEY(`id`)
);

CREATE INDEX IF NOT EXISTS `idx_source` ON `songs` (`source`);
CREATE INDEX IF NOT EXISTS `idx_title_slug` ON `songs` (`title_slug`);
CREATE INDEX IF NOT EXISTS `idx_artist_slug` ON `songs` (`artist_slug`);

CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);
INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'f8ce322c55245ffa8c13896137da76ce');

PRAGMA user_version = 5;
"""

import re

def normalize_artist_and_title(artist: str, title: str) -> tuple[str, str]:
    artist = (artist or "").strip()
    title = (title or "").strip()
    
    # 1. Clean double duplicate name separated by comma (GT artifact: "BILLIE EILISH, BILLIE EILISH")
    if "," in artist:
        parts = [p.strip() for p in artist.split(",") if p.strip()]
        if len(parts) >= 2 and parts[0].lower() == parts[1].lower():
            artist = parts[0]
            
    # 2. Check feat / featuring in artist name
    has_feat_in_title = bool(re.search(r"\b(feat\.?|ft\.?|featuring)\b", title, re.I))
    m_feat = re.search(r"^(.*?)\s+(?:feat\.?|ft\.?|featuring)\s+(.*)$", artist, re.I)
    if m_feat:
        main_art = m_feat.group(1).strip()
        feat_art = m_feat.group(2).strip()
        if not has_feat_in_title and feat_art:
            title = f"{title} (feat. {feat_art})"
        artist = main_art

    return artist, title

def merge_all_batches():
    batch_dbs = sorted(glob.glob(str(DB_BATCHES_DIR / "batch_*.db")))
    if not batch_dbs:
        print(f"No batch databases found in {DB_BATCHES_DIR}")
        return

    # Put batch_ost.db, batch_22.db, and batch_23.db first so curated soundtracks take precedence
    b23_db = str(DB_BATCHES_DIR / "batch_23.db")
    if b23_db in batch_dbs:
        batch_dbs.remove(b23_db)
        batch_dbs.insert(0, b23_db)

    b22_db = str(DB_BATCHES_DIR / "batch_22.db")
    if b22_db in batch_dbs:
        batch_dbs.remove(b22_db)
        batch_dbs.insert(0, b22_db)

    ost_db = str(DB_BATCHES_DIR / "batch_ost.db")
    if ost_db in batch_dbs:
        batch_dbs.remove(ost_db)
        batch_dbs.insert(0, ost_db)

    print(f"Found {len(batch_dbs)} batch databases to merge into {MERGED_DB}...")
    
    if MERGED_DB.exists():
        MERGED_DB.unlink()

    conn_out = sqlite3.connect(MERGED_DB)
    conn_out.executescript(SCHEMA_SQL)
    conn_out.commit()

    total_merged = 0
    seen_urls = set()
    for b_path in batch_dbs:
        b_name = Path(b_path).name
        conn_in = sqlite3.connect(b_path)
        try:
            rows = conn_in.execute("""
                SELECT source, source_url, title, artist, title_slug, artist_slug,
                       version_name, rating, votes, key_base, difficulty, category,
                       region, chords, chord_count, line_count, content, content_hash, scraped_at
                FROM songs
            """).fetchall()
            
            c = conn_out.cursor()
            inserted = 0
            for r in rows:
                try:
                    source, source_url, title, artist, title_slug, artist_slug, version_name, rating, votes, key_base, difficulty, category, region, chords, chord_count, line_count, content, content_hash, scraped_at = r
                    
                    if not source_url or source_url in seen_urls:
                        continue
                    seen_urls.add(source_url)

                    clean_art, clean_title = normalize_artist_and_title(artist, title)
                    if clean_art != artist or clean_title != title:
                        artist = clean_art
                        title = clean_title
                        artist_slug = re.sub(r"[^a-zA-Z0-9]+", "-", artist.lower()).strip("-")
                        title_slug = re.sub(r"[^a-zA-Z0-9]+", "-", title.lower()).strip("-")

                    # Safe fallbacks for NOT NULL columns
                    region = region or "BARAT"
                    chords = chords or ""
                    chord_count = int(chord_count or 0)
                    line_count = int(line_count or 0)
                    content_hash = content_hash or ""
                    scraped_at = scraped_at or ""

                    c.execute("""
                        INSERT INTO songs (
                            source, source_url, title, artist, title_slug, artist_slug,
                            version_name, rating, votes, key_base, difficulty, category,
                            region, chords, chord_count, line_count, content, content_hash, scraped_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        source, source_url, title, artist, title_slug, artist_slug,
                        version_name, rating, votes, key_base, difficulty, category,
                        region, chords, chord_count, line_count, content, content_hash, scraped_at
                    ))
                    inserted += 1
                except Exception:
                    pass
            conn_out.commit()
            total_merged += inserted
            print(f"  + Merged {b_name}: {inserted} songs (total so far: {total_merged})")
        except Exception as e:
            print(f"  ! Error reading {b_name}: {e}")
        finally:
            conn_in.close()

    print("\nPost-processing: Applying Seventeen split & region standardizations...")
    cur = conn_out.cursor()
    # 1. Seventeen Indonesia
    cur.execute("""
        UPDATE songs 
        SET artist = 'Seventeen', artist_slug = 'seventeen-id', region = 'INDO'
        WHERE (artist_slug = 'seventeen-indonesia')
           OR (artist_slug = 'seventeen' AND source = 'GT' AND lower(title) NOT LIKE '%darl%ing%')
    """)
    # 2. SEVENTEEN Korea
    cur.execute("""
        UPDATE songs 
        SET artist = 'SEVENTEEN', artist_slug = 'seventeen-kr', region = 'KOREA'
        WHERE (artist_slug = 'seventeen-세븐틴')
           OR (artist_slug = 'seventeen' AND source = 'GT' AND (lower(title) LIKE '%darl+ing%' OR lower(title) LIKE '%darling%'))
    """)
    # 3. Ifan Seventeen
    cur.execute("UPDATE songs SET region = 'INDO' WHERE lower(artist) LIKE '%ifan seventeen%'")
    # 4. Standardize region INDONESIA -> INDO
    cur.execute("UPDATE songs SET region = 'INDO' WHERE region = 'INDONESIA'")
    # 5. Ensure PRAGMA user_version = 5
    cur.execute("PRAGMA user_version = 5")
    conn_out.commit()
    conn_out.close()

    print(f"\nMerging complete! Total unique songs in merged DB: {total_merged}")

    # Compress to app asset with KordApp XOR masking
    print(f"Compressing to {ASSET_XZ} with XOR mask...")
    temp_xz = TOOLS_DIR / "catalog" / "temp_catalog.db.xz"
    with open(MERGED_DB, "rb") as fin, lzma.open(temp_xz, "wb", preset=5, check=lzma.CHECK_CRC64) as f_xz:
        while chunk := fin.read(256 * 1024):
            f_xz.write(chunk)

    key = b"KORD_KEKALCLIP_2"
    key_len = len(key)
    with open(temp_xz, "rb") as fin, open(ASSET_XZ, "wb") as fout:
        idx = 0
        while chunk := fin.read(65536):
            masked = bytearray(chunk)
            for i in range(len(masked)):
                masked[i] ^= key[(idx + i) % key_len]
            idx += len(chunk)
            fout.write(masked)

    if temp_xz.exists():
        temp_xz.unlink()

    print(f"Compressed and masked catalog successfully updated at {ASSET_XZ} ({os.path.getsize(ASSET_XZ)} bytes)!")

if __name__ == "__main__":
    merge_all_batches()
