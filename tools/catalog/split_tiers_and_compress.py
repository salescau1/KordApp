#!/usr/bin/env python3
"""
split_tiers_and_compress.py
Extracts 3 standalone SQLite modular databases (catalog_a.db, catalog_b.db, catalog_c.db)
from full_merged_catalog.db, preserves Room v6 schema, compresses with LZMA2 (preset 5),
and applies KordApp XOR masking (KORD_KEKALCLIP_2) to produce .db.xz files.
"""
from __future__ import annotations

import lzma
import os
import sqlite3
import sys
import time
from pathlib import Path

if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TOOLS_DIR = Path(__file__).resolve().parent.parent
MASTER_DB = TOOLS_DIR / "catalog" / "full_merged_catalog.db"
GEN_DIR = TOOLS_DIR / "catalog" / "generated"
KEY = b"KORD_KEKALCLIP_2"
KEY_LEN = len(KEY)

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
    `tier` TEXT NOT NULL DEFAULT 'A',
    PRIMARY KEY(`id`)
);

CREATE INDEX IF NOT EXISTS `idx_source` ON `songs` (`source`);
CREATE INDEX IF NOT EXISTS `idx_title_slug` ON `songs` (`title_slug`);
CREATE INDEX IF NOT EXISTS `idx_artist_slug` ON `songs` (`artist_slug`);
CREATE INDEX IF NOT EXISTS `idx_tier` ON `songs` (`tier`);

CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);
INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'a69e31afaf7d1570cec628f49411725a');
PRAGMA user_version = 6;
"""


def extract_and_compress_tier(tier: str, conn_src: sqlite3.Connection) -> dict:
    t_lower = tier.lower()
    target_db_path = GEN_DIR / f"catalog_{t_lower}.db"
    temp_xz_path = GEN_DIR / f"temp_{t_lower}.db.xz"
    final_xz_path = GEN_DIR / f"catalog_{t_lower}.db.xz"

    print(f"\n==================================================")
    print(f"Memproses Paket {tier} -> {target_db_path.name}")
    print(f"==================================================")

    if target_db_path.exists():
        target_db_path.unlink()
    if temp_xz_path.exists():
        temp_xz_path.unlink()
    if final_xz_path.exists():
        final_xz_path.unlink()

    # 1. Buat database baru & inisialisasi schema Room
    conn_dst = sqlite3.connect(target_db_path)
    conn_dst.executescript(SCHEMA_SQL)
    conn_dst.commit()

    # 2. Salin data lagu sesuai tier
    t0 = time.time()
    c_src = conn_src.cursor()
    c_src.execute("""
        SELECT id, source, source_url, title, artist, title_slug, artist_slug,
               version_name, rating, votes, key_base, difficulty, category,
               region, chords, chord_count, line_count, content, content_hash,
               scraped_at, tier
        FROM songs
        WHERE tier = ?
    """, (tier,))

    rows = c_src.fetchall()
    song_count = len(rows)
    print(f"1. Menyalin {song_count:,} lagu Tier '{tier}' dari master database...")

    c_dst = conn_dst.cursor()
    c_dst.executemany("""
        INSERT INTO songs (
            id, source, source_url, title, artist, title_slug, artist_slug,
            version_name, rating, votes, key_base, difficulty, category,
            region, chords, chord_count, line_count, content, content_hash,
            scraped_at, tier
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, rows)
    conn_dst.commit()

    # 3. Verifikasi integritas Room SQLite
    c_dst.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
    c_dst.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'a69e31afaf7d1570cec628f49411725a')")
    c_dst.execute("PRAGMA user_version = 6")
    conn_dst.commit()

    c_dst.execute("VACUUM")
    conn_dst.commit()

    user_ver = c_dst.execute("PRAGMA user_version").fetchone()[0]
    id_hash = c_dst.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]
    verified_count = c_dst.execute("SELECT count(*) FROM songs").fetchone()[0]
    conn_dst.close()

    uncompressed_bytes = target_db_path.stat().st_size
    uncompressed_mb = uncompressed_bytes / (1024 * 1024)
    print(f"   Ekstraksi selesai dalam {time.time() - t0:.1f} dtk. Ukuran uncompressed: {uncompressed_mb:.2f} MB ({uncompressed_bytes:,} bytes)")
    print(f"   Verifikasi: {verified_count:,} lagu, user_version={user_ver}, identity_hash='{id_hash}'")

    # 4. Kompresi XZ (LZMA2 preset 5)
    t1 = time.time()
    print(f"2. Mengompresi ke {temp_xz_path.name} (LZMA2 preset 5)...")
    with open(target_db_path, "rb") as fin, lzma.open(temp_xz_path, "wb", preset=5, check=lzma.CHECK_CRC64) as f_xz:
        while chunk := fin.read(256 * 1024):
            f_xz.write(chunk)
    xz_bytes = temp_xz_path.stat().st_size
    xz_mb = xz_bytes / (1024 * 1024)
    print(f"   Kompresi selesai dalam {time.time() - t1:.1f} dtk. Ukuran XZ mentah: {xz_mb:.2f} MB")

    # 5. XOR masking KordApp
    t2 = time.time()
    print(f"3. Mengaplikasikan proteksi gembok KordApp XOR mask ke {final_xz_path.name}...")
    with open(temp_xz_path, "rb") as fin, open(final_xz_path, "wb") as fout:
        idx = 0
        while chunk := fin.read(65536):
            masked = bytearray(chunk)
            for i in range(len(masked)):
                masked[i] ^= KEY[(idx + i) % KEY_LEN]
            idx += len(chunk)
            fout.write(masked)

    if temp_xz_path.exists():
        temp_xz_path.unlink()

    final_bytes = final_xz_path.stat().st_size
    final_mb = final_bytes / (1024 * 1024)
    print(f"   Selesai proteksi dalam {time.time() - t2:.1f} dtk. File aset final: {final_mb:.2f} MB ({final_bytes:,} bytes)")

    return {
        "tier": tier,
        "db_name": target_db_path.name,
        "xz_name": final_xz_path.name,
        "song_count": verified_count,
        "uncompressed_bytes": uncompressed_bytes,
        "uncompressed_mb": uncompressed_mb,
        "compressed_bytes": final_bytes,
        "compressed_mb": final_mb,
        "compression_ratio": (1 - (final_bytes / uncompressed_bytes)) * 100,
        "user_version": user_ver,
        "identity_hash": id_hash,
    }


def main():
    GEN_DIR.mkdir(parents=True, exist_ok=True)

    print(f"Membuka Master Database: {MASTER_DB}")
    conn_src = sqlite3.connect(MASTER_DB)

    total_master = conn_src.execute("SELECT count(*) FROM songs").fetchone()[0]
    print(f"Total Lagu di Master Catalog: {total_master:,}")

    results = []
    for tier in ["A", "B", "C"]:
        res = extract_and_compress_tier(tier, conn_src)
        results.append(res)

    conn_src.close()

    print("\n" + "=" * 65)
    print("RINGKASAN SISTEM KATALOG MODULAR PER PAKET (KORDAPP)")
    print("=" * 65)
    print(f"{'Paket':<8} | {'Jumlah Lagu':<12} | {'Uncompressed':<15} | {'Compressed (XZ)':<15} | {'Rasio':<8}")
    print("-" * 65)
    total_uncompressed = 0
    total_compressed = 0
    total_songs = 0
    for r in results:
        total_songs += r["song_count"]
        total_uncompressed += r["uncompressed_bytes"]
        total_compressed += r["compressed_bytes"]
        print(f"Paket {r['tier']:<2} | {r['song_count']:>10,}  | {r['uncompressed_mb']:>9.2f} MB    | {r['compressed_mb']:>9.2f} MB    | {r['compression_ratio']:>5.1f}%")
    print("-" * 65)
    print(f"{'TOTAL':<8} | {total_songs:>10,}  | {total_uncompressed / (1024*1024):>9.2f} MB    | {total_compressed / (1024*1024):>9.2f} MB    | {(1 - total_compressed/total_uncompressed)*100:>5.1f}%")
    print("=" * 65)


if __name__ == "__main__":
    main()
