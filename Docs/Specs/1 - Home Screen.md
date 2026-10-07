# 1 — Home Screen

**Status:** DIIMPLEMENTASIKAN — QA PENDING  
**Mockup resmi:** `mockups/Accepted/1 - Home Screen.jpg`  
**Source of truth:** `0 - INDEKS & FAKTA BERSAMA.md`

---

## 1. Tujuan dan kontrak saat ini

Home adalah pintu masuk aplikasi untuk Search, riwayat putar lokal, fakta database offline, dan chart online yang jujur. Katalog chord tetap lokal/offline.

Komposisi dipilih dari lebar window dengan breakpoint `840.dp`, bukan dari orientasi:

- **Compact (< 840dp):** slot top ad, brand/Search, satu lagu Recently Played, selector Spotify/YouTube, daftar chart yang scroll mandiri, dan bottom navigation saat ini.
- **Expanded (>= 840dp):** panel kiri berisi brand/Search, maksimal lima lagu Recently Played, serta jumlah artis/lagu dan ukuran `catalog.db` aktual; panel tengah berisi selector dan Top 20; primary navigation rail berada di kanan dengan urutan `Home | Explore | Setlists | Favorites | Settings`.
- Search dan seluruh detail route tetap menyembunyikan primary navigation sesuai kontrak shell.

## 2. Status implementasi

### Spotify dan YouTube Top 20 — DIIMPLEMENTASIKAN — QA PENDING

- Selector aktif menyediakan Spotify dan YouTube, menyimpan pilihan terakhir, serta memakai snapshot cache terpisah per platform.
- Spotify memakai playlist publik Spotify Top 50 Indonesia melalui dokumen embed tanpa autentikasi; YouTube memakai dokumen playlist publik. Keduanya diambil dengan `HttpURLConnection` tanpa API key atau dependency baru.
- Parser mengambil maksimal 20 ranking nyata. Hasil kosong dianggap gagal, bukan dijadikan chart kosong atau ranking buatan.
- Setiap entry di-resolve ke katalog lokal memakai artist + title persis setelah trim, case-insensitive.
- Entry yang ditemukan menampilkan source chord nyata dan mengaktifkan Favorite/Play.
- Entry yang tidak ditemukan menampilkan `Chords not available`; Favorite/Play tidak aktif.
- Play membuka Song Versions. Pemilihan versi membuka Player.

### Cache dan keadaan gagal — DIIMPLEMENTASIKAN — QA PENDING

- Cache platform terpilih dipublikasikan lebih dahulu dan tetap interaktif selama refresh berjalan di background; status kecil `Memperbarui...` berasal dari `Loaded.isRefreshing`.
- Refresh sukses mengganti snapshot dan menyimpannya. Refresh gagal mempertahankan snapshot terakhir serta mengakhiri status refresh tanpa full-screen loading.
- `Loading` hanya dipakai ketika platform terpilih belum memiliki cache. Tanpa jaringan dan tanpa cache, Home menampilkan `NoData` dengan `Coba lagi`.
- Hasil terlambat dari platform sebelumnya tidak dapat menimpa platform yang sedang dipilih.
- Tidak ada ranking fallback buatan.

### Recently Played, database, Favorite, dan Play — DIIMPLEMENTASIKAN — QA PENDING

- Recently Played berasal dari `userdata.db`, terbaru lebih dahulu dan dibatasi lima oleh ViewModel. Compact menampilkan satu lagu; expanded menampilkan maksimal lima serta empty state jujur jika belum ada riwayat.
- Expanded Home menampilkan jumlah artis, jumlah lagu logis, dan ukuran runtime `catalog.db` dari repository/provider aktual; composable tidak membaca Room/filesystem dan tidak memakai snapshot angka hardcoded.
- Favorite chart persisten memakai `sourceUrl` versi pertama dari urutan katalog deterministik.
- Lagu tersedia membuka Song Versions; lagu yang tidak lagi dapat di-resolve tetap terlihat sebagai unavailable dan tidak dapat dibuka.

### Iklan — DIIMPLEMENTASIKAN — QA PENDING

Top-ad dan native-ad setelah rank #3 adalah slot reservasi. Keduanya collapse ke tinggi nol selama belum ada jaringan iklan/creative sungguhan; tidak ada placeholder atau ruang kosong palsu.

## 3. Bukti implementasi

- `app/src/main/java/com/chordapp/charts/HttpChartRemoteSource.kt`
- `app/src/main/java/com/chordapp/charts/KworbSpotifyParser.kt`
- `app/src/main/java/com/chordapp/charts/ChartRepositoryImpl.kt`
- `app/src/main/java/com/chordapp/viewmodels/HomeViewModel.kt`
- `app/src/main/java/com/chordapp/ui/screens/home/HomeScreen.kt`
- `app/src/test/java/com/chordapp/charts/`
- `app/src/test/java/com/chordapp/viewmodels/HomeViewModelTest.kt`

## 4. Verifikasi capability

- Focused `HomeViewModelTest` lulus untuk cache-first pending refresh, fresh replacement, failure fallback, stale-platform guard, recent-five newest-first, resolusi history, dan runtime catalog stats.
- Unit tests pure helper membuktikan compact pada lebar di bawah `840.dp`, expanded pada `840.dp` ke atas, serta visibility primary navigation untuk top-level/detail routes.
- Full unit suite, `lintDebug`, `assembleDebug`, dan `assembleRelease` lulus pada 10 September 2026; total aktual dicatat hanya di `PROJECT_PLAN.md`.
- Visual expanded/compact serta skenario cache/offline pada device belum ditutup.

## 5. Gap terbuka

1. Verifikasi visual di device pada kedua sisi breakpoint: compact bottom bar, expanded tiga area/right rail, selected tab, detail-route hiding, recent-five, serta scroll mandiri Top 20.
2. Verifikasi device untuk switching Spotify/YouTube, cache tetap terlihat saat refresh, fallback offline dengan cache, serta `NoData` tanpa cache.
3. Integrasi jaringan iklan nyata belum ada; perilaku accepted saat ini adalah collapse.

## 6. Acceptance checklist

- [x] Spotify dan YouTube tersedia melalui sumber playlist publik nyata.
- [x] Resolusi ketersediaan chord memakai artist + title exact-match yang dinormalisasi.
- [x] Lagu tanpa chord lokal tidak dapat difavoritkan/diputar dari chart.
- [x] Cache-first background refresh, fallback, dan `NoData` tanpa cache tersedia.
- [x] Recently Played berasal dari userdata lokal: satu pada compact, maksimal lima pada expanded.
- [x] Expanded Home menampilkan statistik database runtime aktual dan right navigation rail.
- [x] Favorite dan Play untuk row yang tersedia terhubung ke data/navigasi nyata.
- [x] Slot iklan kosong collapse total.
- [x] Full-suite unit, lint, debug build, dan release build memenuhi gate current di `PROJECT_PLAN.md`.
- [ ] Layout dan interaksi Home lulus QA visual di device.
- [ ] Cache/offline/NoData lulus QA di device.
