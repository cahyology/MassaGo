# 🌟 Master Plan: Booking Terapis Langganan, Bookmark di Rating Popup, & Bonus Repeat Order Dinamis

Dokumen ini memuat detail teknis dan alur implementasi fitur **Terapis Langganan (Favorite Booking)**, **Bookmark di Modal Rating**, **Bonus Repeat Order Mitra yang Dinamis**, serta **Panel Kontrol Web Admin**.

---

## 💡 1. Konsep & Rancangan Solusi

### A. ❤️ Bookmark Terapis di Popup Rating (Customer App)
- Saat sesi pijat selesai dan modal rating/ulasan bintang muncul di layar customer:
  - Terdapat kartu toggle interaktif:  
    `[ ❤️ Puas dengan pelayanan [Nama Terapis]? Simpan sebagai Terapis Favorit ]`
  - Jika diaktifkan, ID terapis tersebut disimpan ke daftar favorit pelanggan (tersimpan di lokal & tersinkronisasi ke cloud Supabase).
  - Terapis favorit langsung muncul di seksi **"Terapis Langganan Anda"** di Beranda dan Tab Riwayat.

### B. 🎁 Skema Bonus Repeat Order Dinamis (Bisa Diatur oleh Superadmin)
- Mengganti pemotongan komisi manual dengan **Bonus Loyalitas Mitra (*Loyalty Incentive Bonus*)**.
- Parameter disimpan di Supabase `platform_settings`:
  - `repeat_order_bonus_active`: `true`
  - `repeat_order_bonus_type`: `"FIXED"` (Nominal Rupiah) atau `"PERCENTAGE"` (Persen dari tarif)
  - `repeat_order_bonus_value`: e.g. `15000` (Rp 15.000 per order langganan)
- **Di Web Superadmin:** Disediakan kartu pengaturan interaktif di halaman Pengaturan untuk mengubah nominal/tipe bonus secara fleksibel kapan saja.
- **Di Mitra App:** Notifikasi pesanan langganan berdering dengan badge emas dan struk rincian pendapatan mencantumkan item:
  - Jasa Pijat (80%): Rp 96.000
  - 🎁 **Bonus Loyalitas Pelanggan Langganan**: +Rp 15.000
  - **Total Diterima Mitra**: Rp 111.000

### C. 🛵 Flow Pemesanan Terapis Langganan
1. **Di Beranda Customer:** Seksi slider horizontal **"Terapis Langganan Anda"** dengan tombol cepat `[ Pesan Lagi ]`.
2. **Di Riwayat Customer:** Tombol `[ ⭐ Pesan Lagi Terapis Ini ]` pada setiap kartu order yang telah selesai.
3. **Di Checkout:** Menampilkan kartu terapis terkunci dengan status realtime dan opsi waktu `[ ⚡ Sekarang ]` atau `[ 📅 Jadwalkan Waktu ]`.

---

## 🛠️ 2. Rincian Perubahan File & Komponen

1. **Customer App:**
   - `OrderTrackingScreen.kt`: Tambahkan toggle bookmark favorit di Rating Dialog.
   - `CustomerHomeScreen.kt`: Tambahkan seksi slider horizontal *"Terapis Langganan Anda"*.
   - `CustomerHistoryScreen.kt`: Tambahkan tombol *"⭐ Pesan Lagi Terapis Ini"*.
   - `CheckoutScreen.kt`: Dukung parameter `preferredTherapistId` & `preferredTherapistName` dan kunci kartu terapis.
   - `CustomerOrderRepository.kt`: Logika bookmarking & dispatch order dengan `preferred_therapist_id`.

2. **Mitra App:**
   - `HomeScreen.kt` / Order Dialog: Tampilkan badge emas *"⭐ PESANAN DARI LANGGANAN ANDA"* dan info bonus repeat order.
   - `OrderRepository.kt`: Kalkulasi bonus loyalitas ke saldo wallet terapis saat order selesai.

3. **Web Admin Dashboard (`apps/admin`):**
   - `Settings.tsx`: Tambahkan form pengaturan **"Bonus Repeat Order Pelanggan Langganan"** (Toggle On/Off, Pilihan Tipe: Fixed / Persen, Input Nominal).
