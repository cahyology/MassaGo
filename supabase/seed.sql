-- ====================================================================
-- MassaGo - Seed Initial Data
-- ====================================================================

-- 1. Seed Service Packages
INSERT INTO service_packages (id, name, category, short_description, full_description, benefits, price_60, price_90, price_120, icon_emoji, orders_count)
VALUES
('SRV-TRAD', 'Pijat Tradisional Jawa', 'Tradisional', 'Pijatan urut seluruh tubuh dengan teknik tradisional warisan leluhur.', 'Pijatan menyeluruh untuk melancarkan sirkulasi darah dan melepas otot kaku.', ARRAY['Melancarkan peredaran darah', 'Meredakan pegal linu', 'Membantu tidur nyenyak'], 120000, 160000, 210000, '💆‍♂️', 3820),
('SRV-REFL', 'Refleksi Kaki & Akupresur', 'Refleksi', 'Titik akupresur telapak kaki dan tangan untuk memulihkan vitalitas.', 'Terapi saraf titik refleksi organ tubuh untuk melancarkan metabolisme tubuh.', ARRAY['Meredakan ketegangan kaki', 'Melancarkan metabolisme', 'Mencegah kram'], 110000, 150000, 190000, '🦶', 2150),
('SRV-DEEP', 'Deep Tissue & Sport Massage', 'Kebugaran', 'Tekanan intensif untuk simpul otot kaku setelah olahraga berat.', 'Teknik pijat lapisan jaringan otot dalam untuk mengurai asam laktat.', ARRAY['Memecah simpul otot kaku', 'Mempercepat pemulihan', 'Meningkatkan kelenturan'], 150000, 210000, 260000, '💪', 1840),
('SRV-SCRUB', 'Lulur & Body Scrub Spa', 'Spa & Kulit', 'Pijat relaksasi dipadu scrub rempah organik untuk kulit cerah & halus.', 'Perawatan spa menyeluruh diawali pijatan lembut dan lulur rempah bengkoang/kopi.', ARRAY['Mengangkat sel kulit mati', 'Mencerahkan & melembutkan', 'Sensasi spa di rumah'], 0, 200000, 245000, '✨', 1420),
('SRV-BEKAM', 'Bekam & Kerokan Higienis', 'Kesehatan', 'Pelepas masuk angin dan letih dengan peralatan steril higienis.', 'Terapi kerokan halus atau bekam kering dengan alat steril 1x pakai.', ARRAY['Meredakan masuk angin', 'Melonggarkan pernapasan', 'Peralatan 100% steril'], 135000, 185000, 0, '🍃', 1670),
('SRV-PRENATAL', 'Pijat Relaksasi Ibu Hamil', 'Khusus', 'Pijatan lembut khusus ibu hamil oleh terapis bersertifikasi resmi prenatal.', 'Posisi menyamping yang aman dan nyaman untuk meredakan nyeri pinggang & pegal kaki.', ARRAY['Certified Prenatal Therapist', 'Meredakan nyeri panggul', 'Menenangkan calon ibu'], 175000, 230000, 0, '🤰', 980)
ON CONFLICT (id) DO NOTHING;

-- 2. Seed Promo Vouchers
INSERT INTO promo_vouchers (code, title, description, discount_percent, discount_flat, max_discount, min_spend, is_active)
VALUES
('MASSAGOBARU', 'Diskon 30% Pengguna Baru MassaGo', 'Potongan s/d Rp35.000 untuk pesanan pertama Anda', 30, 0, 35000, 100000, true),
('HEMATWEEKEND', 'Potongan Langsung Rp 20.000', 'Spesial relaksasi akhir pekan minimal order Rp150.000', 0, 20000, 20000, 150000, true),
('SPALUXURY', 'Diskon Rp 50.000 Paket Spa & Scrub', 'Hemat Rp50.000 untuk paket perawatan 120 menit', 0, 50000, 50000, 200000, true)
ON CONFLICT (code) DO NOTHING;
