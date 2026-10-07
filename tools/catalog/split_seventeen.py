import sqlite3
import sys

DB_PATH = r"C:\KordApp-AGY\tools\catalog\full_merged_catalog.db"

def inspect_and_split(dry_run=True):
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("SELECT id, source, artist, artist_slug, title, region FROM songs WHERE lower(artist) LIKE '%seventeen%'")
    rows = cur.fetchall()

    indo_count = 0
    korea_count = 0
    poland_count = 0
    ifan_count = 0
    jun_count = 0
    other_count = 0

    to_update_indo = []
    to_update_korea = []
    to_update_poland = []
    to_update_ifan = []

    for row in rows:
        song_id, source, artist, artist_slug, title, region = row
        low_art = artist.lower()
        low_title = title.lower()

        if "ifan seventeen" in low_art:
            ifan_count += 1
            to_update_ifan.append(song_id)
        elif "poland" in low_art:
            poland_count += 1
            to_update_poland.append(song_id)
        elif "jun (seventeen)" in low_art:
            jun_count += 1
        elif "17 seventeen" in low_art:
            other_count += 1
        elif artist_slug == "seventeen-indonesia":
            indo_count += 1
            to_update_indo.append(song_id)
        elif artist_slug == "seventeen-세븐틴":
            korea_count += 1
            to_update_korea.append(song_id)
        elif artist_slug == "seventeen" and source == "GT":
            # Dari GT: periksa judul
            if "darl+ing" in low_title or "darling" in low_title:
                korea_count += 1
                to_update_korea.append(song_id)
            else:
                indo_count += 1
                to_update_indo.append(song_id)
        else:
            other_count += 1
            print(f"Unmatched: {row}")

    print("=== SUMMARY PEMETAAN ===")
    print(f"Seventeen Indonesia (ID) : {indo_count} entri (target: artist='Seventeen', artist_slug='seventeen-id', region='INDO')")
    print(f"SEVENTEEN Korea (KR)     : {korea_count} entri (target: artist='SEVENTEEN', artist_slug='seventeen-kr', region='KOREA')")
    print(f"Ifan Seventeen (ID)      : {ifan_count} entri (target: region='INDO')")
    print(f"Seventeen Poland         : {poland_count} entri (target: region='BARAT')")
    print(f"Jun / 17 Seventeen       : {jun_count + other_count} entri (tetap)")

    if dry_run:
        print("\n[DRY RUN] Tidak ada perubahan yang disimpan ke database.")
        conn.close()
        return

    print("\n[APPLYING] Memperbarui database...")
    # 1. Update Seventeen Indonesia
    cur.executemany(
        "UPDATE songs SET artist = 'Seventeen', artist_slug = 'seventeen-id', region = 'INDO' WHERE id = ?",
        [(sid,) for sid in to_update_indo]
    )

    # 2. Update SEVENTEEN Korea
    cur.executemany(
        "UPDATE songs SET artist = 'SEVENTEEN', artist_slug = 'seventeen-kr', region = 'KOREA' WHERE id = ?",
        [(sid,) for sid in to_update_korea]
    )

    # 3. Update Ifan Seventeen
    cur.executemany(
        "UPDATE songs SET region = 'INDO' WHERE id = ?",
        [(sid,) for sid in to_update_ifan]
    )

    # 4. Update Seventeen Poland
    cur.executemany(
        "UPDATE songs SET region = 'BARAT' WHERE id = ?",
        [(sid,) for sid in to_update_poland]
    )

    # Pastikan PRAGMA user_version = 5
    cur.execute("PRAGMA user_version = 5")
    conn.commit()

    # Verifikasi hasil
    cur.execute("PRAGMA user_version")
    uv = cur.fetchone()[0]
    print(f"PRAGMA user_version = {uv} (Wajib 5)")

    cur.execute("SELECT artist, artist_slug, region, COUNT(*) FROM songs WHERE artist_slug IN ('seventeen-id', 'seventeen-kr') GROUP BY artist_slug")
    print("\nHasil verifikasi group:")
    for res in cur.fetchall():
        print(f" - {res[0]} (slug: {res[1]}, region: {res[2]}) -> {res[3]} songs")

    conn.close()
    print("\nSelesai! Database berhasil diperbarui.")

if __name__ == "__main__":
    apply = "--apply" in sys.argv
    inspect_and_split(dry_run=not apply)
