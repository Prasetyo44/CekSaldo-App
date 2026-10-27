# Cek Saldo

Aplikasi Android untuk mengecek saldo bank, e-wallet, dan pulsa operator dari satu tempat.
Dibuat dengan Kotlin dan Jetpack Compose.

## Fitur

- Daftar bank (BCA, BRI, Mandiri, BNI, BSI, BTN, CIMB Niaga, Jenius, Jago, SeaBank, blu, dll.),
  e-wallet (GoPay, OVO, DANA, ShopeePay, LinkAja), dan operator (Telkomsel, Indosat, XL, AXIS, Tri, Smartfren).
- **USSD sekali ketuk**, dengan pilihan SIM 1 / SIM 2 untuk ponsel dual SIM.
- **Cek di sini**: balasan USSD satu langkah (mis. cek pulsa) tampil langsung di aplikasi.
- **Buka aplikasi resmi** bank/e-wallet; jika belum terpasang, langsung ke Play Store.
- **SMS banking**: membuka aplikasi SMS dengan nomor dan format pesan yang sudah terisi.
- Favorit, pencarian, filter kategori, serta tambah/ubah/hapus tujuan sendiri.
- Tema terang/gelap dan warna dinamis (Android 12+).

## Privasi

Aplikasi tidak meminta, menyimpan, atau mengirim PIN, password, maupun data rekening, dan tidak
memakai internet. Semua pengecekan dilakukan lewat kanal resmi (USSD, SMS, aplikasi bank).

Izin yang dipakai:
- `CALL_PHONE`: menjalankan kode USSD langsung dan membaca balasan USSD.
- `READ_PHONE_STATE`: memilih SIM 1 / SIM 2.

Tanpa izin tersebut, kode USSD dibuka di aplikasi Telepon untuk ditekan manual.

> Kode USSD dan nama paket aplikasi bisa berubah sewaktu-waktu. Semuanya bisa diubah dari menu ⋮ → Ubah.

## Unduh APK

Setiap push ke `main` membangun APK lewat GitHub Actions dan memperbaruinya di
[Releases → latest](../../releases/tag/latest). Unduh `CekSaldo.apk`, lalu izinkan
"Instal dari sumber tidak dikenal" di Android.

APK release ditandatangani dengan debug key agar bisa langsung dipasang. Untuk rilis ke Play Store,
buat keystore sendiri dan ganti `signingConfig` di `app/build.gradle.kts`.

## Build lokal

Butuh JDK 17 dan Android SDK (API 35).

```bash
./gradlew assembleDebug
# hasil: app/build/outputs/apk/debug/app-debug.apk
```
