// PijatPro Admin Panel - Mock Data Store
const PijatProData = {
    stats: {
        totalGmv: 48650000,
        platformRevenue: 9730000, // 20% platform commission
        therapistEarnings: 38920000, // 80% therapist share
        totalOrdersToday: 142,
        activeOrdersNow: 18,
        onlineTherapists: 46,
        completionRate: 98.6,
        avgRating: 4.96
    },

    therapists: [
        {
            id: "TRP-01",
            name: "Ibu Sri Rahayu, S.Tr.Kes",
            phone: "+62 812-3456-7890",
            gender: "Wanita",
            rating: 4.98,
            completedOrders: 840,
            status: "AVAILABLE", // AVAILABLE, ON_THE_WAY, IN_TREATMENT, SOS, OFFLINE
            battery: 88,
            speedKmH: 0,
            specialty: "Tradisional & Prenatal",
            badge: "Gold Master",
            walletBalance: 1450000,
            currentLocation: { lat: -6.2088, lng: 106.8456, area: "Kuningan, Jakarta Selatan" },
            avatar: "SR",
            kycStatus: "VERIFIED",
            joinDate: "12 Jan 2024"
        },
        {
            id: "TRP-02",
            name: "Bpk. Budi Santoso",
            phone: "+62 813-9876-5432",
            gender: "Pria",
            rating: 4.95,
            completedOrders: 580,
            status: "ON_THE_WAY",
            battery: 74,
            speedKmH: 28,
            specialty: "Deep Tissue & Sport",
            badge: "Gold Master",
            walletBalance: 890000,
            currentLocation: { lat: -6.2255, lng: 106.8095, area: "SCBD Sudirman, Jakarta Selatan" },
            activeOrderId: "ORD-9821",
            avatar: "BS",
            kycStatus: "VERIFIED",
            joinDate: "05 Feb 2024"
        },
        {
            id: "TRP-03",
            name: "Ibu Ratna Dewi",
            phone: "+62 815-1122-3344",
            gender: "Wanita",
            rating: 4.96,
            completedOrders: 670,
            status: "IN_TREATMENT",
            battery: 92,
            speedKmH: 0,
            specialty: "Lulur & Body Scrub",
            badge: "Gold Master",
            walletBalance: 2100000,
            currentLocation: { lat: -6.2615, lng: 106.7811, area: "Pondok Indah, Jakarta Selatan" },
            activeOrderId: "ORD-9820",
            avatar: "RD",
            kycStatus: "VERIFIED",
            joinDate: "20 Nov 2023"
        },
        {
            id: "TRP-04",
            name: "Bpk. Agus Hermawan",
            phone: "+62 818-5566-7788",
            gender: "Pria",
            rating: 4.92,
            completedOrders: 450,
            status: "AVAILABLE",
            battery: 65,
            speedKmH: 0,
            specialty: "Bekam Steril & Akupresur",
            badge: "Silver Pro",
            walletBalance: 620000,
            currentLocation: { lat: -6.1754, lng: 106.8272, area: "Gambir, Jakarta Pusat" },
            avatar: "AH",
            kycStatus: "VERIFIED",
            joinDate: "15 Mar 2024"
        },
        {
            id: "TRP-05",
            name: "Ibu Dewi Lestari",
            phone: "+62 819-9988-7766",
            gender: "Wanita",
            rating: 4.94,
            completedOrders: 390,
            status: "AVAILABLE",
            battery: 82,
            speedKmH: 0,
            specialty: "Refleksi & Tradisional",
            badge: "Silver Pro",
            walletBalance: 480000,
            currentLocation: { lat: -6.1932, lng: 106.8229, area: "Menteng, Jakarta Pusat" },
            avatar: "DL",
            kycStatus: "VERIFIED",
            joinDate: "02 Mei 2024"
        }
    ],

    kycApplicants: [
        {
            id: "KYC-204",
            name: "Hendra Wijaya",
            phone: "+62 812-8877-6655",
            gender: "Pria",
            nik: "3174051208900002",
            city: "Jakarta Barat",
            experienceYears: 6,
            specialties: ["Pijat Tradisional", "Refleksi Kaki", "Bekam Steril"],
            submittedAt: "17 Agu 2026, 06:15",
            status: "PENDING", // PENDING, APPROVED, REJECTED
            documents: {
                ktp: "KTP_Hendra_Wijaya_3174.jpg",
                skck: "SKCK_Polres_Jakbar_2026.pdf",
                certificate: "Sertifikat_BNSP_Akupresur_Level3.pdf",
                photo: "Pas_Foto_Hendra.jpg"
            },
            notes: "Berkas lengkap, sertifikasi BNSP terdaftar aktif."
        },
        {
            id: "KYC-205",
            name: "Nurul Aini, A.Md.Keb",
            phone: "+62 813-7744-1122",
            gender: "Wanita",
            nik: "3275085403920005",
            city: "Jakarta Selatan",
            experienceYears: 8,
            specialties: ["Pijat Ibu Hamil (Prenatal)", "Lulur Spa Organik", "Tradisional Jawa"],
            submittedAt: "17 Agu 2026, 06:45",
            status: "PENDING",
            documents: {
                ktp: "KTP_Nurul_Aini_3275.jpg",
                skck: "SKCK_Polda_Metro_2026.pdf",
                certificate: "Sertifikat_Certified_Prenatal_Maternity.pdf",
                photo: "Pas_Foto_Nurul.jpg"
            },
            notes: "Memiliki latar belakang bidan & sertifikasi resmi prenatal massage."
        },
        {
            id: "KYC-203",
            name: "Siti Maryam",
            phone: "+62 856-2233-4455",
            gender: "Wanita",
            nik: "3171026509880001",
            city: "Jakarta Pusat",
            experienceYears: 4,
            specialties: ["Tradisional", "Refleksi"],
            submittedAt: "16 Agu 2026, 18:20",
            status: "APPROVED",
            documents: {
                ktp: "KTP_Siti_Maryam.jpg",
                skck: "SKCK_Polsek_Gambir.pdf",
                certificate: "Sertifikat_Pelatihan_Urut_Jawa.pdf",
                photo: "Pas_Foto_Siti.jpg"
            },
            notes: "Telah disetujui & akun mitra aktif."
        }
    ],

    liveOrders: [
        {
            id: "ORD-9821",
            customerName: "Amanda Putri Lestari",
            customerPhone: "+62 812-9876-5432",
            location: "Apartemen Sudirman Tower Lt. 12 #12B",
            therapistName: "Bpk. Budi Santoso",
            therapistId: "TRP-02",
            serviceName: "Deep Tissue & Sport Massage",
            durationMinutes: 90,
            aroma: "Lavender Essential Oil",
            status: "ON_THE_WAY", // SEARCHING, MATCHED, ON_THE_WAY, IN_TREATMENT, COMPLETED, CANCELLED
            paymentMethod: "PijatPro Pay",
            totalAmount: 235000,
            therapistShare: 188000, // 80%
            platformFee: 47000,     // 20%
            etaMinutes: 12,
            createdAt: "07:35 WIB",
            notes: "Lantai 12 kamar 12B, bel di sebelah kanan pintu"
        },
        {
            id: "ORD-9820",
            customerName: "Reza Rahardian",
            customerPhone: "+62 818-4455-6677",
            location: "Jl. Metro Kencana VII No. 18, Pondok Indah",
            therapistName: "Ibu Ratna Dewi",
            therapistId: "TRP-03",
            serviceName: "Lulur & Body Scrub Spa",
            durationMinutes: 120,
            aroma: "Minyak Zaitun Murni",
            status: "IN_TREATMENT",
            paymentMethod: "QRIS BCA",
            totalAmount: 265000,
            therapistShare: 212000,
            platformFee: 53000,
            etaMinutes: 0,
            remainingMinutes: 64,
            createdAt: "06:50 WIB",
            notes: "Pagar putih, mohon tekan bel"
        },
        {
            id: "ORD-9819",
            customerName: "Fanny Cindy",
            customerPhone: "+62 819-1122-9988",
            location: "Kemang Village Tower Bloomington #08A",
            therapistName: "Ibu Sri Rahayu",
            therapistId: "TRP-01",
            serviceName: "Pijat Tradisional Jawa",
            durationMinutes: 90,
            aroma: "Rempah Herbal Hangat",
            status: "COMPLETED",
            paymentMethod: "PijatPro Pay",
            totalAmount: 195000,
            therapistShare: 156000,
            platformFee: 39000,
            rating: 5,
            createdAt: "05:30 WIB",
            notes: "Terapis sangat ramah & bersih"
        }
    ],

    withdrawals: [
        {
            id: "WD-8812",
            therapistId: "TRP-01",
            therapistName: "Ibu Sri Rahayu",
            amount: 750000,
            bankName: "BCA",
            accountNumber: "8830129481",
            accountHolder: "SRI RAHAYU",
            requestedAt: "17 Agu 2026, 07:15",
            status: "PENDING" // PENDING, TRANSFERRED, REJECTED
        },
        {
            id: "WD-8811",
            therapistId: "TRP-03",
            therapistName: "Ibu Ratna Dewi",
            amount: 1200000,
            bankName: "Mandiri",
            accountNumber: "1370019284711",
            accountHolder: "RATNA DEWI",
            requestedAt: "17 Agu 2026, 06:30",
            status: "PENDING"
        },
        {
            id: "WD-8810",
            therapistId: "TRP-02",
            therapistName: "Bpk. Budi Santoso",
            amount: 500000,
            bankName: "GoPay Mitra",
            accountNumber: "081398765432",
            accountHolder: "BUDI SANTOSO",
            requestedAt: "16 Agu 2026, 21:00",
            status: "TRANSFERRED"
        }
    ],

    services: [
        {
            id: "SRV-TRAD",
            name: "Pijat Tradisional Jawa",
            category: "Tradisional",
            price60: 120000,
            price90: 160000,
            price120: 210000,
            active: true,
            icon: "💆‍♂️",
            ordersCount: 3820
        },
        {
            id: "SRV-REFL",
            name: "Refleksi Kaki & Akupresur",
            category: "Refleksi",
            price60: 110000,
            price90: 150000,
            price120: 190000,
            active: true,
            icon: "🦶",
            ordersCount: 2150
        },
        {
            id: "SRV-DEEP",
            name: "Deep Tissue & Sport Massage",
            category: "Kebugaran",
            price60: 150000,
            price90: 210000,
            price120: 260000,
            active: true,
            icon: "💪",
            ordersCount: 1840
        },
        {
            id: "SRV-SCRUB",
            name: "Lulur & Body Scrub Spa",
            category: "Spa & Kulit",
            price60: 0,
            price90: 200000,
            price120: 245000,
            active: true,
            icon: "✨",
            ordersCount: 1420
        },
        {
            id: "SRV-BEKAM",
            name: "Bekam & Kerokan Higienis",
            category: "Kesehatan",
            price60: 135000,
            price90: 185000,
            price120: 0,
            active: true,
            icon: "🍃",
            ordersCount: 1670
        },
        {
            id: "SRV-PRENATAL",
            name: "Pijat Relaksasi Ibu Hamil",
            category: "Khusus",
            price60: 175000,
            price90: 230000,
            price120: 0,
            active: true,
            icon: "🤰",
            ordersCount: 980
        }
    ],

    vouchers: [
        {
            code: "PIJATBARU",
            discount: "30% (Maks Rp35.000)",
            minSpend: 100000,
            usedCount: 420,
            status: "AKTIF"
        },
        {
            code: "HEMATWEEKEND",
            discount: "Potongan Rp 20.000",
            minSpend: 150000,
            usedCount: 290,
            status: "AKTIF"
        },
        {
            code: "SPALUXURY",
            discount: "Potongan Rp 50.000",
            minSpend: 200000,
            usedCount: 165,
            status: "AKTIF"
        }
    ],

    sosLogs: [
        {
            id: "SOS-901",
            therapistName: "Ibu Sri Rahayu",
            therapistPhone: "+62 812-3456-7890",
            location: "Jl. HR Rasuna Said Blok X-5 Kav. 12, Jakarta Selatan",
            lat: -6.2198,
            lng: 106.8312,
            time: "14 Agu 2026, 22:15 WIB",
            status: "RESOLVED",
            note: "Kendala ban motor bocor saat malam hari. Tim reaksi cepat mengirim mitra terdekat untuk mendampingi."
        }
    ]
};
