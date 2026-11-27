# PIECER: Project Rules

App pencatat keuangan Android native, 100% gratis, offline-first, tanpa iklan, tanpa akun, tanpa server.

## Stack
- Kotlin, Jetpack Compose, Material 3
- MVVM + Repository, Hilt, Coroutines + Flow
- Room (wajib pakai migration), DataStore
- Min SDK 26

## Aturan penting
- Uang SELALU disimpan sebagai Long (rupiah penuh). DILARANG Double/Float untuk uang.
- Format rupiah: "Rp1.250.000"
- Tanggal disimpan sebagai epoch millis (Long)
- Jangan hardcode warna/ukuran di layar; ambil dari tema
- Teks UI di strings.xml (Bahasa Indonesia)
- Satu tugas = satu fitur kecil, jangan ubah file di luar scope
- INTERNET dan REQUEST_INSTALL_PACKAGES hanya dipakai modul util/update. Tidak ada analitik, iklan, pelacak, atau koneksi lain.
- Setelah selesai, pastikan build sukses

## Desain
Minimalis monokrom, kartu gelap dan putih, sudut membulat besar.
Warna: Ink #0D0D0F, Charcoal #1C1C1E, Graphite #2C2C2E, Snow #F4F4F5,
White #FFFFFF, Mist #E4E4E7, Stone #8E8E93.
Aksen (hanya untuk makna uang): Mint #34D399 (masuk), Coral #F87171 (keluar), Amber #FBBF24 (peringatan).
Font: Plus Jakarta Sans. Kartu 24dp, tombol 16dp.

## Lingkungan
- Windows, JDK 21, ANDROID_HOME=C:\Android
- SDK: platforms;android-34, build-tools;34.0.0, platform-tools
- Gradle tidak terpasang global; build lewat ./gradlew
- Build: ./gradlew assembleDebug | Install ke HP: ./gradlew installDebug