# 💆‍♂️ PijatIn - On-Demand Home Massage & Spa Platform

> **"Layanan Pijat & Spa Panggilan Profesional, Terverifikasi, Higienis & Terpercaya"**

---

## 🏛️ Struktur Ekosistem Monorepo `PijatIn`

```
PijatIn/
├── apps/
│   ├── mitra/             # 📱 Aplikasi Mitra Terapis Android Native (Kotlin + Jetpack Compose)
│   ├── customer/          # 📱 Aplikasi Pelanggan Android Native (Kotlin + Jetpack Compose)
│   └── admin/             # 💻 Web Admin Panel Dashboard (Vercel Ready)
├── backend/               # ⚡ Vercel Serverless Functions & Webhooks (Midtrans, FCM)
├── supabase/              # 🗄️ Database PostgreSQL + PostGIS, RLS, Storage & Realtime
├── build.gradle.kts       # ⚙️ Root Multi-Module Build Configuration
├── settings.gradle.kts    # ⚙️ Project Settings (:apps:mitra & :apps:customer)
└── gradlew                # 🚀 Gradle Wrapper Executable
```

---

## 📱 1. Aplikasi Mitra Terapis (`apps/mitra`)
- **Package ID**: `com.pijatin.mitra`
- **Fitur Utama**:
  - 📡 **Dispatch Radar & Duty Toggle**: Status Siap Kerja dengan animasi glowing pulse.
  - 🔔 **Alert Pesanan Masuk (30 Detik)**: Push Notification Heads-up + Suara Harmonis Chime Ojol & Getaran Haptik.
  - 🗺️ **Peta Navigasi & GPS**: Rute perjalanan menuju alamat pelanggan.
  - 🧼 **SOP Higienitas & Alat Steril**: Ceklis kebersihan sebelum memulai pemijatan.
  - ⏱️ **Timer Terapi Live**: Countdown waktu terapi lingkaran + kontrol suara ambient spa relaksasi.
  - 💳 **Dompet Mitra (PijatIn Pay)**: Pembagian komisi bersih 80% dan penarikan instan ke Bank BCA/Mandiri/BRI/GoPay/DANA.
  - 🚨 **Tombol Darurat SOS 24/7**: Penguncian titik GPS dan koordinasi keamanan.

### Cara Build APK Mitra:
```bash
./gradlew :apps:mitra:assembleDebug
# Output APK: apps/mitra/build/outputs/apk/debug/mitra-debug.apk
```

---

## 📱 2. Aplikasi Pelanggan (`apps/customer`)
- **Package ID**: `com.pijatin.customer`
- **Fitur Utama**:
  - 🏠 **Beranda & Pemilih Lokasi**: Pemilihan titik jemput apartemen/rumah dan saldo PijatIn Pay.
  - 💆‍♀️ **6 Paket Layanan Lengkap**: Tradisional Jawa, Refleksi Kaki, Deep Tissue Sport, Lulur Body Scrub, Bekam Steril, dan Pijat Ibu Hamil (Prenatal).
  - 🌿 **Kustomisasi Lengkap**: Pilihan durasi (60/90/120 mnt), minyak aromaterapi (Zaitun, Lavender, Rempah Hangat, VCO), titik fokus tubuh, tekanan pijat, dan preferensi gender terapis.
  - 🎟️ **Voucher Promo**: Klaim diskon (`PIJATINBARU`, `HEMATWEEKEND`, `SPALUXURY`).
  - 📍 **Live Map Tracking**: Memantau motor terapis bergerak di peta dengan estimasi waktu tiba (ETA).
  - ⭐ **Rating & Tip Digital**: Ulasan bintang 1-5 dan tip nominal instan.

### Cara Build APK Pelanggan:
```bash
./gradlew :apps:customer:assembleDebug
# Output APK: apps/customer/build/outputs/apk/debug/customer-debug.apk
```

---

## 💻 3. Web Admin Panel Dashboard (`apps/admin`)
- **Teknologi**: HTML5 Semantik, Modern Vanilla CSS (*Plus Jakarta Sans*, Glassmorphism, Palet Spa Emerald), Vanilla JS Modular.
- **Fitur Utama**:
  - 📊 **Executive Dashboard**: GMV, Komisi Platform 20%, Hak Mitra 80%, Grafik tren harian.
  - 🗺️ **Peta Operasional Kota (Live Dispatch Map)**: Sebaran terapis (Online, On the way, Terapi, SOS) dengan drawer telemetri baterai dan koordinat.
  - 🪪 **Verifikasi KYC Mitra**: Dokumen viewer KTP, SKCK Kepolisian, Sertifikat BNSP dengan persetujuan 1-klik.
  - 💰 **Persetujuan Tarik Saldo (Disbursement)**: Persetujuan transfer otomatis ke rekening mitra.
  - 🚨 **Pusat Darurat SOS 24/7**: Sirine audio alarm interaktif, koordinat GPS, dan hotline kepolisian.

### Cara Menjalankan Admin Panel:
Cukup buka berkas `apps/admin/index.html` di browser mana pun.

---

## 🗄️ 4. Setup Database Supabase (`supabase/`)
1. Buka [Supabase Dashboard](https://supabase.com).
2. Buat proyek baru (*New Project*).
3. Masuk ke menu **SQL Editor**.
4. Salin dan jalankan seluruh isi berkas `supabase/schema.sql`.
5. Salin dan jalankan isi berkas `supabase/seed.sql` untuk data awal.

---

## ⚡ 5. Deployment ke Vercel (`backend/`)
1. Hubungkan repositori GitHub ini ke dashboard **Vercel**.
2. Vercel akan otomatis mengenali berkas `backend/vercel.json` dan men-deploy Web Admin Panel serta Serverless API dengan HTTPS gratis!
