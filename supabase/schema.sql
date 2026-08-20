-- ====================================================================
-- MassaGo - 100% Complete Production Schema & Direct CRUD Setup
-- Copy and run all SQL statements in Supabase SQL Editor
-- ====================================================================

-- 1. Aktifkan Ekstensi PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Tabel Customers (Pelanggan MassaGo)
CREATE TABLE IF NOT EXISTS customers (
    id TEXT PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    gender VARCHAR(10) DEFAULT 'Wanita',
    wallet_balance BIGINT DEFAULT 0,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2.0 Tabel Customer Saved Addresses (Alamat Tersimpan Pelanggan)
CREATE TABLE IF NOT EXISTS customer_addresses (
    id VARCHAR(50) PRIMARY KEY,
    customer_phone VARCHAR(20) NOT NULL,
    customer_id TEXT,
    title VARCHAR(100) NOT NULL,
    full_address TEXT NOT NULL,
    note TEXT,
    tag VARCHAR(50) DEFAULT 'Rumah',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2.1 Tabel OTP Verifications (Fonnte WhatsApp OTP)
CREATE TABLE IF NOT EXISTS otp_verifications (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    user_type VARCHAR(20) DEFAULT 'CUSTOMER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Tabel Therapists (Mitra Terapis)
CREATE TABLE IF NOT EXISTS therapists (
    id TEXT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    gender VARCHAR(10) DEFAULT 'Pria',
    nik VARCHAR(20),
    ktp_url TEXT,
    selfie_url TEXT,
    status VARCHAR(30) DEFAULT 'PENDING_APPROVAL', -- 'PENDING_APPROVAL', 'VERIFIED', 'REJECTED', 'SUSPENDED'
    bank_name VARCHAR(50) DEFAULT 'BCA',
    account_number VARCHAR(50) DEFAULT '',
    account_holder VARCHAR(100) DEFAULT '',
    rating NUMERIC(3, 2) DEFAULT 5.0,
    review_count INT DEFAULT 0,
    orders_completed INT DEFAULT 0,
    wallet_balance BIGINT DEFAULT 0,
    deposit_balance BIGINT DEFAULT 100000,
    is_online BOOLEAN DEFAULT FALSE,
    duty_status VARCHAR(20) DEFAULT 'OFFLINE',
    max_radius_km INT DEFAULT 10,
    preferred_client_gender VARCHAR(20) DEFAULT 'Semua',
    tier_badge VARCHAR(50) DEFAULT 'Mitra Reguler',
    latitude DOUBLE PRECISION DEFAULT -7.7956,
    longitude DOUBLE PRECISION DEFAULT 110.3695,
    certifications TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Tabel Orders (Realtime Order Dispatch Engine)
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(50) PRIMARY KEY,
    customer_id TEXT,
    therapist_id TEXT,
    service_name VARCHAR(100) NOT NULL,
    duration_minutes INT NOT NULL,
    total_price BIGINT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    payment_status VARCHAR(30) DEFAULT 'UNPAID',
    payment_method VARCHAR(50) DEFAULT 'DOKU_CHECKOUT',
    payment_invoice_url TEXT,
    doku_invoice_id VARCHAR(100),
    customer_name VARCHAR(100),
    customer_phone VARCHAR(20),
    address TEXT,
    gender_preference VARCHAR(30) DEFAULT 'Bebas',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

-- 5. Tabel Service Packages (Katalog Layanan & Tarif)
CREATE TABLE IF NOT EXISTS service_packages (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    short_description TEXT,
    full_description TEXT,
    benefits TEXT[],
    price_60 BIGINT DEFAULT 0,
    price_90 BIGINT DEFAULT 0,
    price_120 BIGINT DEFAULT 0,
    icon_emoji VARCHAR(10) DEFAULT '💆‍♂️',
    orders_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. Tabel Promo Vouchers
CREATE TABLE IF NOT EXISTS promo_vouchers (
    code VARCHAR(30) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    discount_percent INT DEFAULT 0,
    discount_flat BIGINT DEFAULT 0,
    max_discount BIGINT DEFAULT 50000,
    min_spend BIGINT DEFAULT 100000,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. Tabel Reviews (Two-Way Rating System)
CREATE TABLE IF NOT EXISTS reviews (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    reviewer_type VARCHAR(20) NOT NULL, -- 'CUSTOMER' or 'THERAPIST'
    reviewer_id TEXT NOT NULL,
    target_id TEXT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    tags TEXT[],
    review_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 8. Tabel SOS Emergency Logs
CREATE TABLE IF NOT EXISTS sos_emergency_logs (
    id SERIAL PRIMARY KEY,
    sender_type VARCHAR(20) DEFAULT 'THERAPIST', -- 'THERAPIST' atau 'CUSTOMER'
    sender_id TEXT NOT NULL,
    sender_name VARCHAR(100),
    sender_phone VARCHAR(30),
    order_id VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    emergency_type VARCHAR(50) DEFAULT 'EMERGENCY_ASSISTANCE',
    status VARCHAR(30) DEFAULT 'ACTIVE_EMERGENCY', -- 'ACTIVE_EMERGENCY', 'INVESTIGATING', 'RESOLVED', 'FALSE_ALARM'
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 9. Tabel Platform Bank Accounts (Rekening Tujuan Transfer Manual & Top-Up)
CREATE TABLE IF NOT EXISTS platform_bank_accounts (
    id VARCHAR(50) PRIMARY KEY,
    bank_name VARCHAR(50) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 10. Tabel Platform Settings (Konfigurasi QRIS, Komisi, CS WhatsApp)
CREATE TABLE IF NOT EXISTS platform_settings (
    key VARCHAR(50) PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ====================================================================
-- SEED INITIAL DATA (Hanya jika belum ada)
-- ====================================================================

-- Seed Service Packages (Single Valid Service)
INSERT INTO service_packages (id, name, category, short_description, full_description, benefits, price_60, price_90, price_120, icon_emoji, orders_count)
VALUES
('SRV-MASSAGO-01', 'Pijat Tradisional & Kebugaran Keluarga', 'Kebugaran & Relaksasi', 'Pijat tradisional seluruh tubuh untuk meredakan pegal linu, melancarkan sirkulasi darah, dan memulihkan kebugaran tubuh secara profesional.', 'Layanan pemijatan keluarga profesional langsung ke lokasi Anda. Menggabungkan teknik kombinasi urut tradisional, peregangan relaksasi, dan penekanan titik simpul otot kaku dengan minyak aromaterapi alami berkualitas tinggi.', ARRAY['Melancarkan sirkulasi darah & metabolisme', 'Meredakan pegal linu & simpul otot kaku', 'Meningkatkan kualitas tidur & kebugaran', 'Terapis profesional bersertifikasi resmi & SOP higienis'], 100000, 140000, 180000, '💆‍♂️', 0)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  category = EXCLUDED.category,
  short_description = EXCLUDED.short_description,
  full_description = EXCLUDED.full_description,
  benefits = EXCLUDED.benefits,
  price_60 = EXCLUDED.price_60,
  price_90 = EXCLUDED.price_90,
  price_120 = EXCLUDED.price_120,
  icon_emoji = EXCLUDED.icon_emoji;

-- Seed Promo Vouchers
INSERT INTO promo_vouchers (code, title, description, discount_percent, discount_flat, max_discount, min_spend, is_active)
VALUES
('MASSAGOBARU', 'Diskon 30% Pengguna Baru MassaGo', 'Potongan s/d Rp35.000 untuk pesanan pertama Anda', 30, 0, 35000, 100000, true),
('HEMATWEEKEND', 'Potongan Langsung Rp 20.000', 'Spesial relaksasi akhir pekan minimal order Rp150.000', 0, 20000, 20000, 150000, true),
('SPALUXURY', 'Diskon Rp 50.000 Paket Spa & Scrub', 'Hemat Rp50.000 untuk paket perawatan 120 menit', 0, 50000, 50000, 200000, true)
ON CONFLICT (code) DO NOTHING;

-- Seed Default Bank Accounts
INSERT INTO platform_bank_accounts (id, bank_name, account_number, account_holder, is_active)
VALUES
('BANK-BCA', 'Bank Central Asia (BCA)', '8420891234', 'PT PIJATIN INDONESIA SEJAHTERA', true),
('BANK-MANDIRI', 'Bank Mandiri', '1370019283746', 'PT PIJATIN INDONESIA SEJAHTERA', true),
('BANK-BRI', 'Bank Rakyat Indonesia (BRI)', '034101002345538', 'PT PIJATIN INDONESIA SEJAHTERA', true)
ON CONFLICT (id) DO NOTHING;

-- Seed Default Settings
INSERT INTO platform_settings (key, value, description)
VALUES
('qris_image_url', '/qris-massago.png', 'URL / Path Gambar QRIS Statis Platform'),
('qris_merchant_name', 'PIJATIN INDONESIA', 'Nama Merchant Resmi QRIS'),
('qris_nmid', 'ID1020030040050', 'National Merchant ID QRIS'),
('admin_whatsapp', '+6281234567890', 'Nomor WhatsApp Admin Konfirmasi Pembayaran'),
('platform_commission_percent', '20', 'Persentase Bagi Hasil Platform (%)'),
('doku_enabled', 'true', 'Status Aktif Payment Gateway DOKU (true/false)'),
('doku_is_production', 'false', 'Mode Lingkungan DOKU (false = Sandbox, true = Production)'),
('doku_client_id', 'BRN-0242-1787022128265', 'Client ID Merchant DOKU'),
('doku_api_key', 'doku_key_4206227d89174879acb1973748f15cc8', 'API Key DOKU'),
('doku_secret_key', '', 'Secret Key / Shared Key DOKU')
ON CONFLICT (key) DO NOTHING;

-- ====================================================================
-- DISABLE RLS & GRANT DIRECT ACCESS (Agar CRUD Web Admin Berfungsi Langsung)
-- ====================================================================

ALTER TABLE profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE therapists DISABLE ROW LEVEL SECURITY;
ALTER TABLE orders DISABLE ROW LEVEL SECURITY;
ALTER TABLE service_packages DISABLE ROW LEVEL SECURITY;
ALTER TABLE promo_vouchers DISABLE ROW LEVEL SECURITY;
ALTER TABLE reviews DISABLE ROW LEVEL SECURITY;
ALTER TABLE sos_emergency_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE platform_bank_accounts DISABLE ROW LEVEL SECURITY;
ALTER TABLE platform_settings DISABLE ROW LEVEL SECURITY;

GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
