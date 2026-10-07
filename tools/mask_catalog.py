import hashlib

key = b"KORD_KEKALCLIP_2"
key_len = len(key)

orig_hash = hashlib.sha256()
masked_hash = hashlib.sha256()
restored_hash = hashlib.sha256()

src_file = r"C:\KordApp-Test\app\src\main\assets\catalog.db.xz.bak"
dst_file = r"C:\KordApp-Test\app\src\main\assets\catalog.db.xz"

with open(src_file, "rb") as fin, open(dst_file, "wb") as fout:
    idx = 0
    while chunk := fin.read(65536):
        orig_hash.update(chunk)
        masked = bytearray(chunk)
        for i in range(len(masked)):
            masked[i] ^= key[(idx + i) % key_len]
        idx += len(chunk)
        masked_hash.update(masked)
        fout.write(masked)

# Verifikasi restore
with open(dst_file, "rb") as fin:
    idx = 0
    while chunk := fin.read(65536):
        unmasked = bytearray(chunk)
        for i in range(len(unmasked)):
            unmasked[i] ^= key[(idx + i) % key_len]
        idx += len(chunk)
        restored_hash.update(unmasked)

print("Original hash: ", orig_hash.hexdigest())
print("Masked hash:   ", masked_hash.hexdigest())
print("Restored hash: ", restored_hash.hexdigest())
assert orig_hash.hexdigest() == restored_hash.hexdigest()
print("VERIFIKASI SUKSES 100% MATCH!")
