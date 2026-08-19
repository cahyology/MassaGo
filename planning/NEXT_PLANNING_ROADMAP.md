# 🚀 MassaGo Ecosystem - Master Strategy & Next Planning Roadmap

Dokumen ini memuat blueprint arsitektur bisnis, manajemen risiko operasional, penanganan *edge cases*, dan roadmap pengembangan teknologi ekosistem **MassaGo** (Customer App, Mitra App, dan Web Superadmin).

---

## 📌 1. Fondasi Model Bisnis: "Standardized Managed Service with Smart Scale-up"

MassaGo mengadopsi model **Hybrid Smart Platform**:
1. **Fase Peluncuran (Go-to-Market)**:
   - Platform menstandarisasi paket layanan resmi (*Pijat Tradisional, Refleksi, Scrub Body Spa, Deep Tissue*) beserta durasi (60, 90, 120 menit), SOP kebersihan, dan tarif dasar.
   - Menggunakan algoritma **1-Click Auto Match** berdasarkan radius GPS terdekat (5 - 7 km) untuk kecepatan order maksimal (< 10 detik).
2. **Fase Scale-up (Hybrid Mode)**:
   - Membuka fitur **Pilih Terapis Favorit / Terdekat** di mana customer bisa melihat profil mitra, rating bintang, ulasan pelanggan, dan sertifikasi keahlian khusus (*Bekam, Terkilir/Cedera, Pijat Ibu Hamil*).

---

## 🛡️ 2. Trust, Safety & Ethics (Pilar Utama Layanan Panggilan ke Rumah)

Karena layanan berlangsung di ruang privat konsumen, keamanan adalah prioritas mutlak:

### A. Gender Matching Preference
- **Sistem**: Customer wajib memilih preferensi terapis saat checkout: `[ Terapis Wanita ]`, `[ Terapis Pria ]`, atau `[ Bebas / Siapa Saja ]`.
- **Aturan Dispatch**: Sistem hanya akan mengirimkan penawaran order kepada mitra yang sesuai preferensi gender tersebut.

### B. Kebijakan Tegas Anti-Penyalahgunaan (Strict Anti-Harassment)
- **Disclaimer Modal**: Peringatan hukum resmi ditampilkan sebelum konfirmasi pemesanan bahwa MassaGo adalah layanan kebugaran keluarga murni.
- **Panic / SOS Emergency Button**: Tombol darurat di aplikasi Mitra & Customer yang langsung mengirimkan koordinat GPS live dan alert prioritas tinggi ke Superadmin Dashboard & tim satgas lapangan.
- **Blacklist Otomatis**: Pelanggan atau mitra yang terbukti melanggar langsung diblokir NIK/Nomor HP-nya secara permanen.

---

## ⏱️ 3. Penanganan Skenario Tak Terduga Lapangan (*Operational Edge Cases*)

| Kasus Lapangan | Dampak | Solusi Sistem & Aturan Otomatis |
| :--- | :--- | :--- |
| **Customer Tidak Merespons saat Terapis Tiba** (*Ghosting*) | Terapis rugi waktu dan bensin. | Tombol *"Tiba di Lokasi"* mengaktifkan **Grace Period Timer 15 Menit**. Jika tidak ada respon, pesanan dibatalkan dengan kompensasi biaya jalan otomatis ke mitra. |
| **Kendala Perjalanan Mitra** *(Mogok, Ban Bocor, Hujan Badai)* | Keterlambatan kedatangan. | Tombol *"Laporkan Kendala Perjalanan"*. Sistem secara cerdas menawarkan opsi ke customer untuk mengalihkan order ke terapis terdekat lain tanpa mengulang order. |
| **Penambahan Durasi di Tempat** *(On-the-Spot Add-on)* | Transaksi gelap di luar sistem. | Tombol *"Tambah Durasi (+30 / +60 mnt)"* di aplikasi. Tagihan dan bagi hasil 80:20 terhitung otomatis secara legal. |
| **Perbedaan Kondisi Medis Klien** *(Demam tinggi, pasca operasi)* | Risiko kesehatan kontraindikasi pijat. | **Pre-Treatment Screening Checklist** di aplikasi mitra sebelum memulai sesi pijat. |

---

## 💰 4. Keuangan, Deposit & Kontrol Pembagian Hasil

1. **Batas Minimal Deposit Online Mitra**:
   - Untuk mencegah saldo mitra minus akibat sering menerima order tunai (cash), sistem menetapkan batas minimal deposit (misal Rp 20.000).
   - Jika saldo di bawah batas, status mitra otomatis *Off-Duty* hingga melakukan Top-Up deposit via QRIS/Bank Transfer.
2. **Arsitektur Pembayaran Hybrid**:
   - **Tunai di Tempat**: Customer bayar langsung ke mitra setelah selesai. Potongan komisi 20% otomatis dipotong dari saldo dompet aplikasi mitra.
   - **Transfer / QRIS**: Pembayaran masuk ke rekening penampung platform, sistem otomatis mengkreditkan 80% bagian mitra ke dompet digital mitra yang dapat ditarik (*Withdraw*) kapan saja.
3. **Dispute & Komplain Resolution**:
   - Jika ada komplain durasi tidak sesuai, admin dapat mengecek log GPS realtime *Start Service* dan *Finish Service* pada menu admin untuk memberikan keputusan adil.

---

## 🗺️ 5. Logistik, Radius & Surcharge

1. **Radius Dispatch Maksimal**:
   - Dibatasi **5 - 7 km** agar waktu tempuh mitra tidak melebihi 20-30 menit.
2. **Night Surcharge (Insentif Malam)**:
   - Order antara pukul 21:00 - 23:00 dikenakan biaya tambahan tarif malam yang 90% dialokasikan langsung ke mitra sebagai insentif keamanan & transportasi.
3. **Starter Kit Standar Mitra**:
   - Matras lipat portable higienis, kain sprei alas sekali pakai, aromaterapi resmi MassaGo, seragam polo shirt MassaGo berlogo resmi.

---

## 📋 6. Roadmap Pengembangan Teknis Selanjutnya

### Tahap 1: Penyempurnaan Fitur Keamanan & Filter Order ✅ (SELESAI)
- [x] **Smart Gender Matching Filter**: Filter preferensi gender terapis (Wanita/Pria/Bebas) pada form order customer yang otomatis mencocokkan ke gender mitra terapis yang sesuai.
- [x] **Tombol SOS Emergency Terintegrasi**: Tombol darurat 1-tap di app Customer & Mitra lengkap dengan panggilan cepat Polisi 110, Ambulans 118, Satgas MassaGo 24/7, serta siaran GPS darurat ke server.
- [x] **Live SOS Incident Center**: Dashboard Superadmin (`/admin`) terhubung secara real-time ke tabel `sos_emergency_logs` dengan alert visual darurat.
- [x] **Otomasi Grace Period Timer (15 Menit)**: Penghitung waktu tunggu otomatis saat mitra tiba di lokasi (`ARRIVED`) dengan batas toleransi 15 menit sesuai SOP.

### Tahap 2: Manajemen Keuangan & Fleksibilitas Durasi (SELANJUTNYA)
- [ ] **Fitur Tambah Durasi di Tempat (*Extend Order*)**: Pelanggan/terapis dapat menambah durasi (+30 mnt / +60 mnt) saat sesi pijat sedang berlangsung dengan kalkulasi biaya dan bagi hasil otomatis.
- [ ] **Fitur Top-Up Saldo Deposit Mitra**: Integrasi QRIS / Transfer Bank untuk pengisian saldo deposit mitra dengan verifikasi instan.
- [ ] **Form Penarikan Saldo Mitra (*Withdrawal Flow*)**: Penarikan penghasilan terapis ke rekening bank/e-wallet dengan notifikasi persetujuan di Superadmin panel.

### Tahap 3: Scale-Up Mode Hybrid (Direktori Terapis)
- [ ] Halaman profil terapis di aplikasi customer (foto, bio, sertifikasi, rating & ulasan).
- [ ] Opsi pemesanan terapis tertentu (*Direct Booking*).
