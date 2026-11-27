# Panduan Signing Release Piecer

Dokumen ini menjelaskan cara menyiapkan signing untuk build `release` Piecer tanpa menyimpan rahasia atau keystore di dalam repositori.

## Lokasi Berkas `keystore.properties`

Konfigurasi `release` mencari berkas `keystore.properties` **di luar repositori** dengan urutan prioritas berikut:

1. Path pada environment variable `PIECER_KEYSTORE_PROPERTIES`
2. `%USERPROFILE%\.piecer-signing\keystore.properties`

Jika berkas tersebut tidak ditemukan, build `debug` dan `staging` tetap berjalan normal, sedangkan build `release` akan berhenti dengan pesan error yang jelas.

## Format `keystore.properties`

Buat berkas `keystore.properties` di lokasi di atas dengan isi berikut (ganti nilai placeholder dengan milik Anda sendiri):

```properties
storeFile=piecer-release.jks
storePassword=GANTI_DENGAN_PASSWORD_KEYSTORE
keyAlias=piecer
keyPassword=GANTI_DENGAN_PASSWORD_KEY
```

Catatan: `storeFile` dapat berupa path absolut atau path relatif terhadap folder tempat `keystore.properties` berada.

## Cara Membuat Keystore Baru

Jalankan perintah `keytool` (dari JDK 21) di luar direktori repositori, misalnya di `%USERPROFILE%\.piecer-signing\`:

```powershell
mkdir "$env:USERPROFILE\.piecer-signing" -Force
keytool -genkeypair -v `
  -keystore "$env:USERPROFILE\.piecer-signing\piecer-release.jks" `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000 `
  -alias piecer
```

## Pentingnya Backup Keystore

**Simpan salinan cadangan keystore (`.jks`) dan kredensialnya di tempat yang aman dan terpisah (misalnya password manager atau penyimpanan terenkripsi offline).**

- Kehilangan keystore berarti Anda **tidak dapat menandatangani pembaruan aplikasi** dengan sertifikat yang sama.
- Pengguna yang sudah memasang aplikasi tidak akan bisa memperbarui ke versi baru tanpa menghapus instalasi lama (yang berisiko menghilangkan data lokal mereka).
- Jangan pernah meng-commit berkas `.jks`, `.keystore`, atau `keystore.properties` ke Git.

## Perintah Build Release

Setelah `keystore.properties` dan berkas `.jks` tersedia:

```powershell
# Build APK release
./gradlew assembleRelease

# Build Android App Bundle (AAB) release
./gradlew bundleRelease
```
