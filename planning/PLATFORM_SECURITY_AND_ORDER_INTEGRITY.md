# 🛡️ MassaGo - Platform Integrity, Safety & Order Architecture Masterplan

Dokumen ini adalah cetak biru (*master blueprint*) komprehensif mengenai **Integritas Platform, Keamanan Dua Arah (Mitra & Customer), Pencegahan Transaksi di Luar Aplikasi (*Disintermediation*), serta Arsitektur Pemesanan Hybrid**.

---

## 📌 1. Pencegahan Transaksi di Luar Aplikasi (*Platform Leakage Defense*)

Dalam bisnis *on-demand home massage*, pelanggan yang sudah merasa cocok dengan terapis tertentu memiliki kecenderungan untuk bertransaksi langsung (pribadi/WhatsApp) di pemesanan berikutnya. MassaGo menerapkan strategi terintegrasi untuk mencegah kebocoran platform:

### A. Fitur "Terapis Langganan / Favorit" (*In-App Re-Order*)
* **Masalah:** Pelanggan pindah ke WhatsApp karena di aplikasi tidak bisa memilih terapis yang sama.
* **Solusi Produk:**
  - Tambahkan tombol **"Pesan Lagi dengan Terapis Ini"** di riwayat pesanan dan halaman profil favorit.
  - Pelanggan dapat melihat kalender ketersediaan jam kerja terapis tersebut dan melakukan booking resmi di dalam aplikasi.

### B. Skema Komisi Bertingkat (*Tiered Loyalty Commission*)
* **Order Acak / Auto-Match:** Potongan platform standar **20%** (Mitra 80%).
* **Order dari Pelanggan Langganan (Customer memilih langsung):** Potongan platform dipangkas menjadi **hanya 8%** (Mitra 92%).
* **Dampak:** Mitra merasa sangat dihargai dan tidak terbebani biaya aplikasi, sehingga lebih memilih bertransaksi resmi demi kemudahan pencatatan dan keamanan.

### C. Value Tambahan yang Hilang Jika Transaksi di Luar (*Unmatched Platform Benefits*)
1. **Bagi Pelanggan:**
   - **MassaGo Poin & Cashback:** Poin reward di setiap order yang dapat ditukar dengan voucher diskon.
   - **Garansi Kepuasan & Asuransi:** Penggantian terapis atau refund jika layanan tidak sesuai standar.
   - **Pembayaran Fleksibel:** QRIS, e-Wallet, Virtual Account tanpa perlu repot menyediakan uang tunai pas.
2. **Bagi Mitra:**
   - **Jaminan Kompensasi Pembatalan (*Anti-Ghosting*):** Jika pelanggan offline membatalkan sepihak, terapis rugi bensin. Di aplikasi, mitra tetap menerima kompensasi biaya jalan.
   - **Poin Level & Jenjang Karir (Tier Silver/Gold/Platinum):** Order resmi menaikkan poin reputasi dan membuka akses bonus bulanan serta asuransi kesehatan/kecelakaan kerja.
   - **Fitur Keamanan Aktif:** Perlindungan tombol SOS dan monitoring satgas admin.

### D. Sensor Kata Kunci Chat (*Smart Keyword Filtering*)
- Sistem *In-App Chat* secara otomatis mendeteksi dan menyensor teks ajakan transaksi ilegal (seperti: *"transfer BCA"*, *"WA pribadi"*, *"08..."*, *"japri"*, *"tanpa aplikasi"*).
- Menampilkan peringatan edukatif otomatis di layar chat mengenai bahaya transaksi di luar sistem resmi.

---

## 🛵 2. Arsitektur Pemesanan Hybrid: "Pesan Sekarang" vs "Jadwalkan Nanti"

MassaGo menggabungkan fleksibilitas pemesanan instan ala ojek online dengan kepastian waktu ala salon booking:

```
                              [ CHECKOUT PESANAN ]
                                       │
                 ┌─────────────────────┴─────────────────────┐
                 ▼                                           ▼
      ⚡ [ PESAN SEKARANG ]                       📅 [ JADWALKAN NANTI ]
      (On-Demand Instant)                         (Slot Waktu Mendatang)
  - Cari terapis terdekat aktif               - Pilih Tanggal (Hari ini / Besok / Lusa)
  - Terapis langsung meluncur                 - Pilih Jam Slot (contoh: 19.00 - 20.30)
  - Estimasi tiba: 15-30 menit                - Terapis konfirmasi jadwal
```

### A. Flow 1: On-Demand Instan (Pesan Sekarang)
- **Target Pengguna:** Pelanggan yang lelah mendadak sepulang kerja, pegal sehabis olahraga, atau tamu hotel yang butuh terapis saat itu juga.
- **Mekanisme:** Radar pencarian radius 5-7 km, notifikasi broadcast 30 detik ke mitra On-Duty terdekat, live GPS tracking rute sepeda motor menuju lokasi.

### B. Flow 2: Scheduled Booking (Jadwalkan Waktu)
- **Target Pengguna:** Ibu rumah tangga, pekerja yang merencanakan istirahat akhir pekan, atau paket keluarga berdurasi panjang (90-120 menit).
- **Mekanisme:**
  - Pelanggan memilih slot waktu (contoh: Besok pukul 19:00 WIB).
  - Pesanan masuk ke tab **"Jadwal Mendatang"** di aplikasi Mitra yang bersangkutan.
  - Sistem mengirimkan alarm & push notification pengingat otomatis H-1 jam sebelum waktu penjemputan.

---

## 🚻 3. Two-Way Gender Matching & Perlindungan Mitra Wanita

Untuk menjamin keselamatan dan kenyamanan kerja mitra terapis (khususnya wanita):

### A. Filter Preferensi di Sisi Mitra (Pengaturan Akun):
- **Terapis Wanita:**
  - Pilihan 1: `Hanya Menerima Pelanggan Wanita & Keluarga` (Rekomendasi / Default).
  - Pilihan 2: `Menerima Semua Gender (Pria & Wanita)`.
- **Terapis Pria:**
  - Pilihan 1: `Menerima Semua Gender`.
  - Pilihan 2: `Hanya Pelanggan Pria`.

### B. Deklarasi Ganda saat Checkout Pelanggan:
1. **Jenis Kelamin Penerima Layanan:** `[ Pria ]` / `[ Wanita ]` / `[ Pasutri / Keluarga ]`
2. **Preferensi Gender Terapis:** `[ Terapis Wanita ]` / `[ Terapis Pria ]` / `[ Bebas ]`

### C. Aturan Dispatch Server:
- Jika Penerima Layanan = `Pria` dan Preferensi Terapis = `Wanita`, sistem **HANYA** mengirimkan order ke terapis wanita yang secara eksplisit mengaktifkan opsi *"Menerima Pelanggan Pria"*. Terapis wanita dengan opsi *"Hanya Wanita"* 100% aman dari order tersebut.

---

## 🚫 4. Protokol Anti-Pelecehan & Hak Tolak di Tempat (*Right to Refuse*)

### A. Skenario Ketidaksesuaian Data di Lokasi
- Jika pelanggan mengisi data "Wanita", namun setibanya di lokasi ternyata yang hendak dipijat adalah seorang **pria sendirian di kamar privat tertutup tanpa pendamping**:
  1. Terapis berhak menolak masuk dan menekan tombol **"Batalkan: Gender Pelanggan Tidak Sesuai"** di aplikasi Mitra.
  2. Order otomatis dibatalkan tanpa penalti atau penurunan rating bagi mitra.
  3. Mitra tetap menerima uang kompensasi transport.
  4. Akun pelanggan langsung diberi tanda bendera merah (*Flagged for Audit*) oleh Superadmin.

### B. Zero-Tolerance & Disclaimer Hukum
- Banner konfirmasi tegas sebelum pemesanan:
  > ⚖️ **Komitmen Layanan Profesional:**  
  > *"MassaGo adalah platform pijat kesehatan keluarga murni. Segala bentuk pelecehan seksual, asusila, atau kekerasan akan dilaporkan ke pihak kepolisian dan akun Anda akan diblokir permanen."*

### C. Safety Center & SOS Alert 24/7
- Tombol SOS darurat yang langsung menyiarkan titik koordinat live, merekam log audio darurat, dan menghubungkan panggilan ke hotline keamanan internal serta pihak berwajib (Polisi 110).

---

## 🔒 5. Pencegahan Order Fiktif (*Anti-Fraud System*)

1. **Wajib Pembayaran Digital / DP untuk Akun Baru (Order 1 s/d 3):**
   - Pengguna baru wajib membayar melalui QRIS / e-Wallet / Transfer Bank sebelum terapis meluncur. Metode Cash on Delivery (COD) hanya terbuka untuk akun yang sudah memiliki riwayat transaksi sukses minimal 3x.
2. **Verifikasi WhatsApp Asli (OTP Engine):**
   - Mencegah akun bot dan pendaftaran massal dengan nomor acak.
3. **Validasi Geocoding & Alamat Lengkap:**
   - Titik pin peta harus memiliki kecocokan radius dengan alamat jalan nyata, bukan area kosong atau koordinat mencurigakan.
4. **Single Device Session Binding (Auto-Kick):**
   - 1 Akun hanya dapat aktif di 1 perangkat fisik untuk mencegah penyalahgunaan akun atau pemesanan spam serentak.

---

## 🗺️ 6. Matriks Rencana Implementasi

| Modul | Komponen | Prioritas | Status |
| :--- | :--- | :---: | :---: |
| **Keamanan** | Single Device Session & Auto-Kick | High | ✅ Selesai |
| **Keamanan** | Two-Way Gender Preference Filter | High | ⏳ Tahap 1 |
| **Keamanan** | Tombol Pembatalan Ketidaksesuaian Gender + Kompensasi | High | ⏳ Tahap 1 |
| **Order Flow** | Dual Flow Checkout (Pesan Sekarang vs Jadwalkan) | High | ⏳ Tahap 2 |
| **Retensi** | Fitur "Pesan Lagi Terapis Favorit" | Medium | ⏳ Tahap 2 |
| **Finansial** | Komisi Diskon untuk Repeat Customer (8%) | Medium | ⏳ Tahap 2 |
| **Anti-Fraud** | Kebijakan Digital Payment untuk Akun Baru | High | ⏳ Tahap 3 |
| **Chat** | Filter Sensor Kontak & Transaksi Ilegal | Medium | ⏳ Tahap 3 |
