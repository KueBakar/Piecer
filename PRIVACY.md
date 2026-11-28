# Kebijakan Privasi PIECER

**Ringkasnya: Datamu tinggal di HP-mu.**

## Data yang Dikumpulkan

PIECER **tidak** mengumpulkan, menyimpan di server, menjual, atau membagikan data pribadi maupun data keuangan Anda kepada siapa pun.

- Tidak ada pembuatan akun atau login.
- Tidak ada server backend milik developer.
- Tidak ada iklan, SDK analitik, atau pelacak pihak ketiga.
- Seluruh transaksi, dompet, anggaran, target, utang, dan pengaturan disimpan 100% secara lokal di perangkat Anda.
- Laporan crash hanya disimpan secara lokal di perangkat, tidak pernah dikirim otomatis, dan hanya dibagikan atas aksi Anda sendiri.

## Koneksi Jaringan (Hanya Pembaruan Aplikasi)

Pada build rilis, satu-satunya koneksi internet yang dapat dilakukan PIECER adalah menghubungi GitHub (`github.com`, `*.github.io`, `objects.githubusercontent.com`) untuk:

1. Memeriksa berkas metadata versi (`update.json`).
2. Mengunduh berkas APK pembaruan apabila Anda memilih untuk memperbarui.

Saat permintaan HTTPS tersebut berlangsung, PIECER tidak mengirimkan data pribadi, ID perangkat, atau data keuangan apa pun. Namun, sebagai bagian dari protokol jaringan standar, **GitHub dapat melihat alamat IP perangkat Anda** dan header HTTP standar (seperti versi aplikasi).

Fitur pemeriksaan pembaruan otomatis **mati secara default** sampai Anda menyetujuinya, dan dapat dimatikan atau dinyalakan kapan saja melalui **Pengaturan → Pembaruan**.

## Izin Aplikasi

- `INTERNET`: Hanya digunakan oleh modul pembaruan (`util/update`) untuk memeriksa `update.json` dan mengunduh APK rilis dari GitHub melalui HTTPS.
- `REQUEST_INSTALL_PACKAGES`: Hanya digunakan untuk memasang APK pembaruan resmi yang telah diverifikasi ukuran, SHA-256, dan tanda tangannya.
- `POST_NOTIFICATIONS` & `RECEIVE_BOOT_COMPLETED`: Opsional, hanya untuk menjadwalkan ulang pengingat lokal di perangkat.

## Kontak

Jika ada pertanyaan terkait privasi, silakan hubungi melalui repositori GitHub resmi PIECER.
