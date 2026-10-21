# ChordApp Catalog Architecture

## 1. Otoritas dan Ruang Lingkup
Dokumen ini adalah kontrak data katalog tunggal. Requirement layar berada di `Docs/Specs/`; status implementasi dan validasi berada di `PROJECT_PLAN.md` serta `DEFINITION_OF_DONE.md`.

Urutan otoritas untuk data:
1. `app/src/main/assets/catalog.db` — katalog runtime yang dikirim bersama aplikasi.
2. `tools/catalog/rebuild_catalog.py` — kontrak rebuild/tooling aktual.
3. `catalog/UG.db`, `catalog/GT.db`, `catalog/chordtela.db` — input master immutable.

Runtime tidak membaca tiga master database. Tooling boleh membangun kandidat katalog, tetapi tidak boleh memodifikasi master atau mengganti aset runtime tanpa audit dan promosi eksplisit.

## 2. Snapshot Runtime Authoritative

| Fakta | Nilai |
|---|---:|
| Path aset | `app/src/main/assets/catalog.db` |
| Ukuran | 297.267.200 bytes |
| SHA-256 | `e5bf721425f3695deb21d374545f9b20379af8e66582687b25140a3581d12cc4` |
| Tabel | `songs` saja |
| Indeks | `idx_artist_slug`, `idx_source`, `idx_title_slug` |
| Total row/version | 69.847 |
| Lagu logis `(artist_slug, title_slug)` | 65.538 |
| Artist logis `artist_slug` | 20.778 |
| Lagu multi-source | 2.323 |
| Lagu multi-row | 3.065 |
| Maksimum row per lagu logis | 5 |

### Distribusi source

| Source | Rows |
|---|---:|
| ChordTela | 41.973 |
| GT | 19.753 |
| UG | 8.121 |

### Distribusi region

| Region | Rows |
|---|---:|
| `id` | 37.051 |
| `en` | 32.793 |
| `jp` | 3 |

Jumlah source dan region masing-masing sama dengan 69.847. Angka UI harus dihitung dari database aktual; snapshot ini bukan izin untuk hardcode statistik di aplikasi.

## 3. Source Master vs Tooling vs Runtime

### Source master
- `catalog/UG.db`
- `catalog/GT.db`
- `catalog/chordtela.db`

Ketiganya adalah input immutable. Row source/version tidak boleh dihapus, digabung isinya, atau ditimpa untuk deduplikasi.

### Tooling
`tools/catalog/rebuild_catalog.py` membaca master dan cache presentation, menulis kandidat ke `tools/catalog/generated/catalog.db`, menolak cache parsial, mengaudit jumlah source, dan menolak overwrite output yang sudah ada. Kandidat tidak otomatis menjadi aset produksi.

Rebuild script saat ini menghitung:

```text
content_hash = SHA-256(content UTF-8 persis sebagaimana tersimpan)
```

Script mengaudit `source_url` dan `content` agar byte-for-byte sama dengan master. Karena itu, jangan menyebut `content_hash` sebagai hash konten ternormalisasi. Klaim normalisasi baru sah jika script, audit, dan test benar-benar diubah serta diverifikasi.

`presentation_content` adalah output tooling untuk presentation yang disimpan di SQLite kandidat. Entity Room runtime saat ini memetakan kontrak kolom yang digunakan aplikasi dan tidak menjadikan tooling sebagai dependency runtime.

### Runtime
Aset produksi adalah `app/src/main/assets/catalog.db`. Room menyalinnya melalui `createFromAsset("catalog.db")` ke database aplikasi bernama `catalog.db`. Aplikasi memperlakukan katalog sebagai read-only: tidak ada DAO insert/update/delete dan tidak boleh memakai destructive migration.

## 4. Schema Runtime
Database runtime hanya memiliki tabel `songs` dengan kolom:

```text
id INTEGER PRIMARY KEY AUTOINCREMENT
source TEXT NOT NULL
source_url TEXT NOT NULL UNIQUE
title TEXT NOT NULL
artist TEXT NOT NULL
title_slug TEXT NOT NULL
artist_slug TEXT NOT NULL
version_name TEXT NULL
rating REAL NULL
votes INTEGER NULL
key_base TEXT NULL
difficulty TEXT NULL
category TEXT NULL
region TEXT NOT NULL
chords TEXT NOT NULL
chord_count INTEGER NOT NULL
line_count INTEGER NOT NULL
content TEXT NOT NULL
presentation_content TEXT NOT NULL
content_hash TEXT NOT NULL
scraped_at TEXT NOT NULL
```

Indeks yang sah dan wajib cocok dengan entity Room:

```text
idx_artist_slug ON songs(artist_slug)
idx_source       ON songs(source)
idx_title_slug   ON songs(title_slug)
```

Tidak ada tabel runtime `artists`, view, atau FTS yang boleh diasumsikan. Artist dan lagu logis dihitung dari `songs`.

## 5. Identitas dan Pengelompokan

### Row catalog / versi
Setiap row adalah satu record source/version. `source_url` adalah referensi stabil utama. `id` hanya surrogate lokal SQLite dan dapat berubah ketika katalog dibangun ulang.

### Lagu logis

```text
(artist_slug, title_slug)
```

Daftar lagu boleh menampilkan satu item per lagu logis, tetapi semua row dasarnya harus tetap tersedia di Song Versions.

### Artist logis

```text
artist_slug
```

Artist yang hadir di beberapa source tetap satu artist logis untuk Artist Directory.

### Larangan
- Jangan menyimpan `songs.id` sebagai persistent userdata identity.
- Jangan menghapus source lain ketika lagu yang sama cocok lintas source.
- Jangan mencampur chord/lirik dari dua row.
- Jangan menganggap overlap sebagai error data.

## 6. Metadata dan Konten
Source memiliki metadata berbeda. Kolom optional harus tetap NULL bila tidak tersedia. UI hanya boleh menampilkan nilai nyata yang diizinkan spec.

Jangan mengarang album, durasi, BPM/tempo, genre, review count, rating, votes, key, difficulty, category, tags, atau Songsterr. `category` yang kosong tidak boleh diinferensikan dari artist/title.

`chords` adalah data daftar chord yang dipakai antara lain untuk diagram. `content` dipertahankan persis dari master. Player merender secara source-faithful: bracket yang memang tersimpan dipertahankan, sementara baris chord polos tidak diberi bracket buatan. Isi chord/lirik tidak boleh dicetak ke log, terminal, laporan, atau chat.

## 7. Kontrak Query
- Total row: `COUNT(*)`.
- Total lagu logis: grouping/count berdasarkan `(artist_slug, title_slug)`.
- Total artist: `COUNT(DISTINCT artist_slug)`.
- Artist browsing: group berdasarkan `artist_slug`; urutan alfabet case-insensitive bila relevan.
- Song list/search: group berdasarkan `(artist_slug, title_slug)`.
- Song Versions: semua row dengan pasangan slug tersebut.
- Pemilihan versi/Player: lookup exact `source_url`.

Query layar tidak boleh menghapus row source/version secara permanen. Pengelompokan hanya membentuk presentation model.

## 8. Userdata Terpisah
Mutable state berada di Room database `userdata.db`, bukan katalog. Schema saat ini berisi:
- `setlists`
- `setlist_items`
- `favorite_songs`
- `play_history`

`userdata.db` menyimpan data buatan user dan referensi/snapshot minimal; tidak menyimpan isi chord/lirik.

### Referensi Favorites dan Setlist items
Simpan:

```text
source_url
source
content_hash
artist_slug
title_slug
transpose
capo
```

Setlist item juga menyimpan `position` dan `note`. `play_history` memakai `source_url` sebagai primary reference dan menyimpan metadata display minimal.

### Relasi koleksi
- Setlists merupakan koleksi datar tanpa Songbook.
- Menghapus Setlist menghapus itemnya melalui cascade.
- Migrasi userdata harus eksplisit; jangan memakai destructive fallback generik. Migration `3 → 4` mendapat izin untuk mereset hanya domain Setlists development sambil mempertahankan Favorites dan play history.

## 9. Resolution Cascade
Saat referensi Favorites/Setlist tidak lagi exact pada katalog baru:
1. exact `source_url`;
2. pasangan `(source, content_hash)` dalam lagu logis yang sama;
3. versi lain dari source yang sama untuk `(artist_slug, title_slug)`;
4. versi dari source lain untuk lagu logis yang sama.

Langkah 4 adalah substitusi dan harus diberitahukan kepada user. Jika tidak ditemukan, pertahankan userdata dan tampilkan unavailable; jangan menghapus diam-diam. Transpose, capo, note, posisi, dan konteks koleksi harus dipertahankan sepanjang resolusi/substitusi yang valid.

## 10. Batas Layer
Akses data mengikuti:

```text
UI → ViewModel → Repository → Room DAO / platform
```

- UI mengirim intent dan merender state; tidak membuka DB/network langsung.
- ViewModel mengelola state dan lifecycle layar.
- Repository mengoordinasikan query katalog, userdata, resolution, chart/cache, atau platform.
- DAO katalog hanya query read-only.
- DAO userdata menangani mutasi state user dan transaksi.

Chart dan iklan adalah data online terpisah; keduanya bukan bagian katalog chord dan kegagalannya tidak boleh membuat katalog/Player unusable offline.

## 11. Rebuild, Promosi, dan Integrity Gate
Sebelum kandidat mengganti `app/src/main/assets/catalog.db`, verifikasi minimum:
1. 69.847 row dan source counts tepat: ChordTela 41.973, GT 19.753, UG 8.121.
2. Hanya tabel `songs` dan tiga indeks yang dikontrakkan.
3. `source_url` unik dan seluruh source/version dipertahankan.
4. `artist_slug`, `title_slug`, `content`, metadata optional, dan source identity terjaga.
5. `content_hash` cocok dengan algoritme rebuild aktual.
6. Region totals dapat dipertanggungjawabkan.
7. Representative multi-source/multi-row songs mengekspos seluruh versi.
8. Entity Room dapat membuka prepackaged DB tanpa schema mismatch.
9. Test repository/DAO dan resolution cascade lulus.
10. Favorites/Setlists bertahan atau resolve dengan UX substitusi/unavailable yang benar.
11. Ukuran dan SHA-256 aset baru dicatat sebagai snapshot authoritative baru.

Build tooling harus gagal daripada diam-diam membuang row. Promosi aset adalah tindakan eksplisit setelah audit; master tetap tidak berubah.

## 12. Status Kontrak Saat Ini
- Katalog runtime dan kontrak schema/count di atas: **DIIMPLEMENTASIKAN & TERVERIFIKASI**.
- Runtime read-only dan pemisahan `userdata.db`: **DIIMPLEMENTASIKAN & TERVERIFIKASI** pada struktur kode/schema.
- UX resolusi update catalog secara menyeluruh: **PARSIAL**; cascade repository ada, tetapi pembuktian update-survival dan seluruh state substitusi/unavailable belum lengkap.
- Release/update gate end-to-end: **BELUM DIIMPLEMENTASIKAN**.

Status implementasi, baseline validasi, dan release/update backlog secara keseluruhan hanya berada di `PROJECT_PLAN.md`; kontrak data ini tidak boleh dipakai untuk mengklaim seluruh aplikasi hijau.