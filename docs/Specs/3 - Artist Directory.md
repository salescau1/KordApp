# 3 — Artist Directory

**Status:** DIIMPLEMENTASIKAN — QA PENDING  
**Mockup resmi:** `mockups/Accepted/3 - Artist Directory.jpg`  
**Source of truth:** `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Tujuan dan kontrak saat ini

Artist Directory menampilkan lagu logis milik satu artis dari katalog lokal.

Flow utama: `Search/Explore → Artist Directory → Song Versions → Player`.

Bottom navigation disembunyikan pada layar detail ini dan Back mengikuti back stack melalui kontrol Back bersama.

## 2. Status implementasi

### Pengelompokan lagu — DIIMPLEMENTASIKAN — QA PENDING

- Lagu artis diambil dari katalog lokal.
- Query mengelompokkan berdasarkan `artist_slug` + `title_slug`, sehingga satu lagu logis tampil satu kali walaupun memiliki beberapa versi/source.
- Versi tiap lagu tetap tersedia untuk menghasilkan badge source.
- Penggabungan lintas source bergantung pada slug artis/title katalog yang sama; kualitas normalisasi katalog di luar layar ini tetap menentukan hasil akhir.

### Sort dan filter — DIIMPLEMENTASIKAN — QA PENDING

- Satu-satunya urutan adalah **A–Z**, case-insensitive.
- Tidak ada mode `Popular`, karena katalog tidak menyediakan data popularitas nyata.
- Field `Filter songs` menyaring daftar yang sudah dimuat berdasarkan judul secara lokal; hasil tetap A–Z.

### Kartu lagu dan navigasi — DIIMPLEMENTASIKAN — QA PENDING

- Seluruh kartu, termasuk judul, adalah satu target tap dan membuka Song Versions.
- Kartu tidak langsung membuka Player.
- Badge bawah adalah label source nyata (`UG`, `GT`, `ChordTela`), didedup per lagu.
- Badge source bersifat non-interaktif dan tidak memilih versi.
- `display_version` hanya digunakan pada Song Versions, bukan badge Artist Directory.

### Data yang tidak boleh dikarang

Tidak menampilkan album, durasi, popularitas, Follow, atau source yang tidak dimiliki lagu.

## 3. Bukti implementasi

- `app/src/main/java/com/chordapp/catalog/CatalogDao.kt`
- `app/src/main/java/com/chordapp/repositories/CatalogRepository.kt`
- `app/src/main/java/com/chordapp/viewmodels/ArtistDirectoryViewModel.kt`
- `app/src/main/java/com/chordapp/ui/screens/artist/ArtistDirectoryScreen.kt`
- `app/src/main/java/com/chordapp/ui/components/SourceBadge.kt`
- `app/src/main/java/com/chordapp/ui/navigation/AppNavigation.kt`

## 4. Verifikasi capability

- Requirement utama telah dicocokkan dengan kode.
- Belum ada device QA yang membuktikan layout, filter, grouping lintas source, dan alur navigasi secara end-to-end.
- Baseline project current hanya dicatat di `PROJECT_PLAN.md`; karena gap khusus layar masih ada, status tetap `DIIMPLEMENTASIKAN — QA PENDING`.

## 5. Gap terbuka

1. QA device untuk jumlah lagu, filter lokal, urutan A–Z, tap seluruh kartu, Back, dan bottom-nav tersembunyi.
2. Verifikasi dataset bahwa kategori/non-artis tidak bocor sebagai artis. Layar memakai hasil katalog apa adanya dan tidak memiliki blacklist UI tersendiri.
3. Verifikasi sampel lintas source bahwa slug katalog benar-benar menyatukan artis dan lagu yang sama.

## 6. Acceptance checklist

- [x] Lagu logis dikelompokkan berdasarkan artist/title slug.
- [x] Daftar memakai A–Z saja.
- [x] Filter judul lokal tersedia.
- [x] Seluruh kartu/judul membuka Song Versions.
- [x] Badge source didedup dan non-interaktif.
- [x] Bottom navigation disembunyikan pada layar detail.
- [ ] Dataset kategori/non-artis telah diverifikasi tidak bocor.
- [ ] Penggabungan lintas source telah diverifikasi pada sampel katalog.
- [ ] Layout dan interaksi lulus QA di device.
