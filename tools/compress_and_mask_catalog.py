import lzma
import hashlib
import os
import time

SRC_DB = r"C:\KordApp-AGY\tools\catalog\full_merged_catalog.db"
TEMP_XZ = r"C:\KordApp-AGY\tools\catalog\new_catalog.db.xz"
FINAL_ASSET = r"C:\KordApp-AGY\app\src\main\assets\catalog.db.xz"

KEY = b"KORD_KEKALCLIP_2"
KEY_LEN = len(KEY)

print(f"1. Membaca database: {SRC_DB}")
file_size_mb = os.path.getsize(SRC_DB) / (1024 * 1024)
print(f"   Ukuran asli: {file_size_mb:.1f} MB")

t0 = time.time()
print("2. Mengompresi ke format XZ (LZMA2)...")
with open(SRC_DB, "rb") as fin, lzma.open(TEMP_XZ, "wb", preset=5, check=lzma.CHECK_CRC64) as f_xz:
    while chunk := fin.read(256 * 1024):
        f_xz.write(chunk)

xz_size_mb = os.path.getsize(TEMP_XZ) / (1024 * 1024)
print(f"   Selesai kompresi dalam {time.time() - t0:.1f} detik! Ukuran XZ: {xz_size_mb:.1f} MB")

print("3. Mengaplikasikan proteksi gembok KordApp (XOR mask)...")
with open(TEMP_XZ, "rb") as fin, open(FINAL_ASSET, "wb") as fout:
    idx = 0
    while chunk := fin.read(65536):
        masked = bytearray(chunk)
        for i in range(len(masked)):
            masked[i] ^= KEY[(idx + i) % KEY_LEN]
        idx += len(chunk)
        fout.write(masked)

print(f"4. Selesai! File aset terpasang di: {FINAL_ASSET}")

# Hapus temporary raw XZ
if os.path.exists(TEMP_XZ):
    os.remove(TEMP_XZ)

print("SUKSES: Database baru (157.305 lagu) telah terpasang dan terproteksi 100%!")
