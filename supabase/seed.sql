-- ====================================================================
-- MassaGo - Seed Initial Data
-- ====================================================================

-- 1. Seed Service Packages (Single Valid Service)
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

-- 2. Seed Promo Vouchers
INSERT INTO promo_vouchers (code, title, description, discount_percent, discount_flat, max_discount, min_spend, is_active)
VALUES
('MASSAGOBARU', 'Diskon 30% Pengguna Baru MassaGo', 'Potongan s/d Rp35.000 untuk pesanan pertama Anda', 30, 0, 35000, 100000, true),
('HEMATWEEKEND', 'Potongan Langsung Rp 20.000', 'Spesial relaksasi akhir pekan minimal order Rp150.000', 0, 20000, 20000, 150000, true),
('SPALUXURY', 'Diskon Rp 50.000 Paket Spa & Scrub', 'Hemat Rp50.000 untuk paket perawatan 120 menit', 0, 50000, 50000, 200000, true)
ON CONFLICT (code) DO NOTHING;
