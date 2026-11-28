# PIECER

**Datamu tinggal di HP-mu.**

PIECER adalah aplikasi pencatat keuangan Android native yang 100% gratis, offline-first, tanpa iklan, tanpa akun, dan tanpa server.

## Fitur Utama

- **Catat Cepat & Transaksi**: Pemasukan, pengeluaran, dan transfer antar dompet dengan nominal Rupiah presisi (`Long`), dilengkapi Ketik cepat pintar (angka lisan, banyak item sekaligus, belajar dari riwayat) serta catat dengan suara lewat pengenal suara bawaan HP.
- **Banyak Dompet**: Tunai, rekening bank, dan e-wallet.
- **Anggaran & Target**: Pantau batas pengeluaran per kategori dan target tabungan.
- **Pieces (Alokasi)**: Bagi saldo ke pos-pos khusus.
- **Utang & Split Bill**: Catat piutang, cicilan, dan patungan.
- **Transaksi Berulang & Pengingat Harian**: Otomatis catat tagihan rutin.
- **Statistik & Net Worth**: Grafik pengeluaran dan ringkasan kekayaan bersih.
- **Backup & Ekspor CSV**: Cadangkan data ke berkas JSON lokal atau ekspor ke CSV.
- **Keamanan**: Kunci PIN/biometrik, blokir tangkapan layar, dan dukungan tema terang/gelap.

## Cara Pasang (APK)

1. Unduh berkas `PIECER-v<versi>.apk` dari halaman [Releases](https://github.com/KueBakar/Piecer/releases) atau halaman unduhan.
2. Buka berkas APK tersebut di perangkat Android Anda.
3. Jika diminta, aktifkan izin **Izinkan dari sumber ini** (*Install unknown apps*) untuk browser atau pengelola berkas Anda.
4. **Peringatan Google Play Protect**: Karena PIECER didistribusikan secara mandiri di luar Google Play Store, Play Protect mungkin menampilkan peringatan *"Aplikasi tidak dikenal"* atau *"Play Protect tidak mengenali developer aplikasi ini"*. Pilih **Tetap pasang** (*Install anyway*) jika Anda mengunduh APK resmi dari repositori ini. Anda juga dapat memverifikasi kecocokan checksum **SHA-256** APK dengan yang tertera pada rilis.

## Catatan Privasi

- Tidak ada akun, tidak ada server backend, tidak ada iklan, dan tidak ada analitik/pelacak.
- Seluruh data keuangan tersimpan lokal di perangkat Anda.
- Satu-satunya koneksi jaringan pada build rilis adalah pemeriksaan dan pengunduhan pembaruan aplikasi dari GitHub (opsional, default mati sampai Anda menyetujuinya, dan dapat dimatikan kapan saja di **Pengaturan → Pembaruan**).
- Fitur **Catat dengan suara** memakai layanan pengenalan suara bawaan HP (biasanya Google) tanpa izin `RECORD_AUDIO`; PIECER tidak merekam atau menyimpan audio, hanya memakai teks hasilnya (sebagian HP memprosesnya lewat internet).
- Baca detail lengkapnya di [PRIVACY.md](PRIVACY.md).

## Cara Build dari Kode Sumber

Prasyarat: JDK 21, Android SDK (API 34), dan `ANDROID_HOME` terkonfigurasi.

```powershell
# Jalankan unit test
./gradlew testDebugUnitTest

# Build APK debug
./gradlew assembleDebug

# Build APK staging
./gradlew assembleStaging

# Jalankan lint
./gradlew lintDebug
```

## Langkah Rilis Versi Baru

> **Penting — Keystore Harus Sama:** Semua versi rilis **wajib** ditandatangani dengan berkas keystore (`.jks`) yang sama. Jika keystore berubah atau hilang, pengguna tidak dapat memasang pembaruan di atas aplikasi yang sudah terpasang dan pembaruan dalam aplikasi akan menolak APK tersebut. Lihat [SIGNING.md](SIGNING.md) untuk panduan penyiapan `keystore.properties` di luar repositori.

1. Naikkan `versionCode` dan `versionName` di `app/build.gradle.kts` (skema `versionCode`: `mayor * 10000 + minor * 100 + patch`, misalnya `1.5.0` → `10500`).
2. Perbarui catatan rilis di `CHANGELOG.md`.
3. Pastikan `keystore.properties` sudah tersedia (di `%USERPROFILE%\.piecer-signing\keystore.properties` atau env `PIECER_KEYSTORE_PROPERTIES`).
4. Jalankan skrip rilis:
   ```powershell
   ./release.ps1
   ```
5. Skrip akan menjalankan unit test, membangun `assembleRelease`, menyalin APK ke `dist/PIECER-v<versi>.apk`, mencetak ukuran dan SHA-256, serta memperbarui `docs/update.json`.
6. Unggah `dist/PIECER-v<versi>.apk` ke GitHub Releases dengan tag `v<versi>`, lalu commit dan push `docs/update.json` dan `CHANGELOG.md` agar GitHub Pages menyajikan metadata terbaru.

## Lisensi

PIECER dilisensikan di bawah [GNU General Public License v3.0 or later (GPL-3.0-or-later)](LICENSE).
