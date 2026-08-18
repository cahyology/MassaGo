'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import {
  Sparkles,
  ShieldCheck,
  Clock,
  MapPin,
  Star,
  Download,
  Smartphone,
  CheckCircle2,
  ChevronRight,
  ArrowRight,
  Menu,
  X,
  HeartHandshake,
  Users,
  Award,
  HelpCircle,
  PhoneCall,
  MessageCircle,
  Zap,
  TrendingUp,
  Sliders,
  DollarSign,
  ChevronDown,
  ExternalLink
} from 'lucide-react';

export default function LandingPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [activeCategory, setActiveCategory] = useState<'all' | 'tradisional' | 'refleksi' | 'spa' | 'khusus'>('all');
  const [faqOpen, setFaqOpen] = useState<number | null>(0);
  const [downloadModalOpen, setDownloadModalOpen] = useState(false);
  const [selectedApp, setSelectedApp] = useState<'customer' | 'mitra'>('customer');

  const services = [
    {
      id: 'trad-1',
      title: 'Pijat Tradisional Jawa',
      category: 'tradisional',
      tagline: 'Favorit Kebugaran',
      duration: '90 Menit',
      price: 'Rp 140.000',
      rating: '4.95',
      reviews: '3.820',
      badge: 'Best Seller',
      description: 'Teknik urut mendalam warisan leluhur untuk melancarkan sirkulasi darah, meredakan pegal linu, dan memulihkan stamina tubuh.',
      features: ['Minyak aromaterapi alami', 'Kain sprei steril sekali pakai', 'Fokus punggung, pinggang & kaki', 'Terapis tersertifikasi']
    },
    {
      id: 'ref-1',
      title: 'Refleksi Kaki & Akupresur',
      category: 'refleksi',
      tagline: 'Relaksasi Instan',
      duration: '60 Menit',
      price: 'Rp 110.000',
      rating: '4.94',
      reviews: '2.150',
      badge: 'Cepat & Segar',
      description: 'Stimulasi titik meridian saraf pada telapak kaki dan betis untuk detoksifikasi, meningkatkan metabolisme, dan kualitas tidur.',
      features: ['Krim refleksi herbal', 'Relaksasi pundak & leher bonus', 'Meredakan kaki bengkak & kram', 'Cocok setelah bekerja seharian']
    },
    {
      id: 'deep-1',
      title: 'Deep Tissue & Sport Massage',
      category: 'khusus',
      tagline: 'Otot Kaku & Pegal Kronis',
      duration: '90 Menit',
      price: 'Rp 160.000',
      rating: '4.96',
      reviews: '1.420',
      badge: 'Intensif',
      description: 'Penekanan ritmik berenergi tinggi untuk mengurai simpul otot kaku (muscle knots), ketegangan bahu, dan pemulihan pasca olahraga.',
      features: ['Balm hangat pereda nyeri', 'Peregangan sendi (stretching)', 'Teknik pemijatan presisi', 'Direkomendasikan untuk atlet/pekerja kantor']
    },
    {
      id: 'spa-1',
      title: 'Body Scrub & Lulur Spa',
      category: 'spa',
      tagline: 'Perawatan Kulit & Relaksasi',
      duration: '120 Menit',
      price: 'Rp 195.000',
      rating: '4.98',
      reviews: '980',
      badge: 'Premium',
      description: 'Kombinasi pijat relaksasi seluruh tubuh dilanjutkan dengan lulur scrub rempah organik untuk mengangkat sel kulit mati dan mencerahkan.',
      features: ['Scrub rempah tradisional', 'Pijat relaksasi 60 menit', 'Body oil pelembap kulit', 'Aroma floral lavender menenangkan']
    },
    {
      id: 'trad-2',
      title: 'Pijat Relaksasi Aromaterapi',
      category: 'tradisional',
      tagline: 'Anti Stres & Insomnia',
      duration: '60 Menit',
      price: 'Rp 125.000',
      rating: '4.92',
      reviews: '1.890',
      badge: 'Calming',
      description: 'Usapan ritmis berirama santai dengan essential oil lavender & sandalwood untuk menenangkan saraf tegang dan melepas stres harian.',
      features: ['Pilihan minyak esensial organik', 'Tekanan pijatan lembut-sedang', 'Menenangkan pikiran & saraf', 'Suasana spa hotel di rumah']
    },
    {
      id: 'khusus-1',
      title: 'Pijat Ibu Hamil & Pasca Melahirkan',
      category: 'khusus',
      tagline: 'Khusus Terapis Bersertifikasi',
      duration: '90 Menit',
      price: 'Rp 175.000',
      rating: '4.99',
      reviews: '640',
      badge: 'Sertifikasi Khusus',
      description: 'Posisi miring ergonomis yang aman untuk meredakan nyeri pinggang, punggung, serta kaki bengkak pada ibu hamil dan pasca melahirkan.',
      features: ['Bantal khusus penopang perut', 'Minyak alami hypoallergenic', 'Terapis bidan/sertifikasi khusus', 'Hanya untuk usia kehamilan aman']
    }
  ];

  const filteredServices = activeCategory === 'all'
    ? services
    : services.filter(s => s.category === activeCategory);

  const faqs = [
    {
      q: 'Bagaimana cara memesan layanan pijat di MassaGo?',
      a: 'Sangat mudah! Anda cukup mengunduh aplikasi MassaGo (atau klik tombol Pesan), pilih jenis layanan yang Anda inginkan (misal Pijat Tradisional atau Refleksi), tentukan waktu (sekarang atau terjadwal), lalu sistem akan mencocokkan mitra terapis terdekat yang langsung meluncur ke lokasi Anda dalam 20-30 menit.'
    },
    {
      q: 'Apakah saya bisa memilih jenis kelamin (gender) terapis?',
      a: 'Ya, tentu saja! Keamanan dan kenyamanan Anda adalah prioritas utama kami. Di aplikasi, Anda dapat memfilter apakah ingin dilayani oleh Terapis Wanita, Terapis Pria, atau Bebas.'
    },
    {
      q: 'Perlengkapan apa saja yang dibawa oleh mitra terapis MassaGo?',
      a: 'Mitra kami membawa starter kit higienis lengkap, meliputi matras lipat portable, kain alas sprei bersih sekali pakai (disposable sheet), minyak aromaterapi bersertifikasi, hand sanitizer, masker, dan handuk bersih.'
    },
    {
      q: 'Metode pembayaran apa saja yang didukung?',
      a: 'MassaGo mendukung metode pembayaran fleksibel: Tunai langsung di tempat setelah pemijatan selesai, QRIS (Gopay, OVO, Dana, ShopeePay, BCA, Mandiri), Transfer Bank, serta Saldo MassaGo Pay.'
    },
    {
      q: 'Bagaimana standar keamanan dan etika di MassaGo?',
      a: 'MassaGo adalah brand layanan kesehatan dan kebugaran profesional keluarga. Seluruh mitra terapis telah melalui verifikasi identitas (KTP/SKCK) dan uji kompetensi. Kami menerapkan kebijakan Strict Anti-Harassment dan tombol SOS Darurat untuk melindungi konsumen maupun mitra.'
    },
    {
      q: 'Bagaimana cara bergabung menjadi Mitra Terapis MassaGo?',
      a: 'Jika Anda memiliki keahlian memijat, Anda dapat mengunduh aplikasi MassaGo Mitra, mendaftar langsung dengan mengisi data diri dan sertifikat keahlian, atau menghubungi tim pendaftaran mitra kami. Dapatkan bagi hasil adil 80%, jam kerja fleksibel, dan bonus insentif.'
    }
  ];

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col font-sans selection:bg-emerald-500 selection:text-white">
      
      {/* 🟢 TOP ANNOUNCEMENT BAR */}
      <div className="bg-gradient-to-r from-emerald-700 via-emerald-600 to-teal-700 text-white text-xs py-2 px-4 text-center font-medium shadow-inner flex items-center justify-center gap-2">
        <span className="bg-amber-400 text-emerald-950 font-bold px-2 py-0.5 rounded-full text-[10px] uppercase tracking-wider animate-pulse">
          PROMO BARU
        </span>
        <span>Diskon 30% Pengguna Pertama dengan kode voucher: <strong className="underline tracking-wider font-mono">MASSAGOBARU</strong></span>
      </div>

      {/* 🧭 NAVIGATION HEADER */}
      <header className="sticky top-0 z-40 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800/80 transition-colors duration-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between">
          
          {/* Brand Logo */}
          <Link href="/" className="flex items-center gap-3 group">
            <div className="w-12 h-12 rounded-2xl bg-emerald-600 shadow-md shadow-emerald-500/20 flex items-center justify-center p-1.5 transition-transform duration-300 group-hover:scale-105">
              <img src="/logo.svg" alt="MassaGo Logo" className="w-full h-full object-contain filter drop-shadow-sm" />
            </div>
            <div>
              <span className="text-2xl font-black tracking-tight text-slate-900 dark:text-white flex items-center gap-1.5">
                MassaGo
                <span className="inline-block w-2 h-2 rounded-full bg-emerald-500"></span>
              </span>
              <span className="block text-[10px] font-semibold tracking-widest text-emerald-600 dark:text-emerald-400 uppercase -mt-1">
                HOME MASSAGE & WELLNESS
              </span>
            </div>
          </Link>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-slate-600 dark:text-slate-300">
            <a href="#layanan" className="hover:text-emerald-600 dark:hover:text-emerald-400 transition-colors">Layanan</a>
            <a href="#keunggulan" className="hover:text-emerald-600 dark:hover:text-emerald-400 transition-colors">Keunggulan</a>
            <a href="#cara-kerja" className="hover:text-emerald-600 dark:hover:text-emerald-400 transition-colors">Cara Kerja</a>
            <a href="#mitra" className="hover:text-emerald-600 dark:hover:text-emerald-400 transition-colors">Gabung Mitra</a>
            <a href="#faq" className="hover:text-emerald-600 dark:hover:text-emerald-400 transition-colors">FAQ</a>
          </nav>

          {/* Header Action Buttons */}
          <div className="hidden lg:flex items-center gap-3">
            <Link
              href="/admin"
              className="px-4 py-2 text-xs font-semibold text-slate-700 dark:text-slate-200 bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 rounded-xl transition-all flex items-center gap-1.5 border border-slate-200 dark:border-slate-700"
            >
              <Sliders className="w-3.5 h-3.5 text-emerald-500" />
              Portal Admin
            </Link>

            <button
              onClick={() => {
                setSelectedApp('customer');
                setDownloadModalOpen(true);
              }}
              className="px-5 py-2.5 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-md shadow-emerald-500/25 rounded-xl transition-all transform hover:-translate-y-0.5 flex items-center gap-2"
            >
              <Download className="w-4 h-4" />
              Download Aplikasi
            </button>
          </div>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 focus:outline-none"
            aria-label="Toggle Navigation Menu"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {/* Mobile Dropdown Drawer */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 px-6 py-6 space-y-4 shadow-xl animate-fadeIn">
            <nav className="flex flex-col space-y-3 text-base font-medium text-slate-700 dark:text-slate-200">
              <a href="#layanan" onClick={() => setMobileMenuOpen(false)} className="py-1 hover:text-emerald-600">Layanan Pijat</a>
              <a href="#keunggulan" onClick={() => setMobileMenuOpen(false)} className="py-1 hover:text-emerald-600">Keunggulan Kami</a>
              <a href="#cara-kerja" onClick={() => setMobileMenuOpen(false)} className="py-1 hover:text-emerald-600">Cara Memesan</a>
              <a href="#mitra" onClick={() => setMobileMenuOpen(false)} className="py-1 hover:text-emerald-600">Gabung Jadi Mitra</a>
              <a href="#faq" onClick={() => setMobileMenuOpen(false)} className="py-1 hover:text-emerald-600">Tanya Jawab (FAQ)</a>
            </nav>
            <div className="pt-4 border-t border-slate-200 dark:border-slate-800 flex flex-col gap-2.5">
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  setSelectedApp('customer');
                  setDownloadModalOpen(true);
                }}
                className="w-full py-3 text-sm font-bold text-white bg-emerald-600 rounded-xl text-center shadow-md shadow-emerald-500/20 flex items-center justify-center gap-2"
              >
                <Download className="w-4 h-4" />
                Download Aplikasi Customer
              </button>
              <Link
                href="/admin"
                onClick={() => setMobileMenuOpen(false)}
                className="w-full py-2.5 text-xs font-semibold text-slate-700 dark:text-slate-200 bg-slate-100 dark:bg-slate-800 rounded-xl text-center flex items-center justify-center gap-1.5"
              >
                <Sliders className="w-3.5 h-3.5 text-emerald-500" />
                Masuk Portal Superadmin
              </Link>
            </div>
          </div>
        )}
      </header>

      {/* 🚀 HERO SECTION */}
      <section className="relative overflow-hidden pt-12 pb-20 lg:pt-20 lg:pb-32 bg-gradient-to-b from-emerald-50/50 via-slate-50 to-white dark:from-slate-900 dark:via-slate-950 dark:to-slate-950">
        
        {/* Background Ambient Glows */}
        <div className="absolute top-10 left-1/2 -translate-x-1/2 w-[600px] lg:w-[1000px] h-[350px] bg-gradient-to-tr from-emerald-400/20 via-teal-400/15 to-emerald-600/10 blur-[120px] pointer-events-none rounded-full" />
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
            
            {/* Left Column: Headlines & CTAs */}
            <div className="lg:col-span-7 space-y-8 text-center lg:text-left">
              
              {/* Trust Badge Pill */}
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-emerald-100/80 dark:bg-emerald-950/60 border border-emerald-300/60 dark:border-emerald-700/50 text-emerald-800 dark:text-emerald-300 text-xs font-semibold shadow-sm">
                <Sparkles className="w-4 h-4 text-emerald-600 dark:text-emerald-400 animate-spin" style={{ animationDuration: '6s' }} />
                <span>Layanan Home Massage & Spa Panggilan Terpercaya #1</span>
              </div>

              {/* Main Headline */}
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-900 dark:text-white leading-[1.15]">
                Pijat & Spa Profesional <br className="hidden sm:inline" />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-600 via-teal-500 to-emerald-500">
                  Langsung ke Pintu Rumah Anda.
                </span>
              </h1>

              {/* Subheadline */}
              <p className="text-base sm:text-lg text-slate-600 dark:text-slate-300 max-w-2xl mx-auto lg:mx-0 leading-relaxed font-normal">
                Tanpa macet, tanpa antre. Nikmati relaksasi tubuh berkualitas bintang 5 bersama 
                <strong> mitra terapis tersertifikasi</strong>, higienis, ramah, dan siap datang ke rumah, apartemen, atau hotel Anda.
              </p>

              {/* Key Trust Stats Pill Grid */}
              <div className="grid grid-cols-3 gap-3 pt-2 max-w-lg mx-auto lg:mx-0 text-left">
                <div className="p-3 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-sm">
                  <div className="flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-black text-xl">
                    <Clock className="w-4 h-4" /> 25 Mnt
                  </div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400 font-medium mt-0.5">Rata-rata Tiba</div>
                </div>

                <div className="p-3 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-sm">
                  <div className="flex items-center gap-1 text-amber-500 font-black text-xl">
                    <Star className="w-4 h-4 fill-amber-400 text-amber-400" /> 4.95
                  </div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400 font-medium mt-0.5">10.000+ Ulasan</div>
                </div>

                <div className="p-3 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl shadow-sm">
                  <div className="flex items-center gap-1 text-teal-600 dark:text-teal-400 font-black text-xl">
                    <ShieldCheck className="w-4 h-4" /> 100%
                  </div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400 font-medium mt-0.5">Aman & Terverifikasi</div>
                </div>
              </div>

              {/* CTA Action Buttons */}
              <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-2">
                <button
                  onClick={() => {
                    setSelectedApp('customer');
                    setDownloadModalOpen(true);
                  }}
                  className="w-full sm:w-auto px-7 py-4 text-sm font-bold text-white bg-emerald-600 hover:bg-emerald-700 shadow-xl shadow-emerald-600/30 rounded-2xl transition-all transform hover:-translate-y-1 flex items-center justify-center gap-3 group"
                >
                  <Download className="w-5 h-5 transition-transform group-hover:translate-y-0.5" />
                  <span>Download Aplikasi MassaGo</span>
                </button>

                <a
                  href="https://wa.me/6281298765432?text=Halo%20Admin%20MassaGo,%20saya%20ingin%20memesan%20layanan%20pijat%20panggilan."
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full sm:w-auto px-6 py-4 text-sm font-semibold text-slate-700 dark:text-slate-200 bg-white hover:bg-slate-100 dark:bg-slate-800 dark:hover:bg-slate-700 border border-slate-200 dark:border-slate-700 rounded-2xl transition-all shadow-sm flex items-center justify-center gap-2"
                >
                  <MessageCircle className="w-5 h-5 text-emerald-500" />
                  <span>Pesan via WhatsApp CS</span>
                </a>
              </div>

              <div className="flex items-center justify-center lg:justify-start gap-6 text-xs text-slate-500 dark:text-slate-400 font-medium">
                <span className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" /> Tanpa Biaya Transport Tambahan
                </span>
                <span className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" /> Garansi Terapis Pengganti
                </span>
              </div>
            </div>

            {/* Right Column: Live Visual Mockup Card */}
            <div className="lg:col-span-5 relative flex justify-center">
              
              {/* Outer Decorative Glow */}
              <div className="absolute inset-0 bg-gradient-to-tr from-emerald-500/20 to-teal-500/30 rounded-3xl blur-2xl transform scale-95" />

              {/* Smartphone Frameless Preview Container */}
              <div className="relative w-full max-w-sm bg-white dark:bg-slate-900 border border-slate-200/90 dark:border-slate-800 rounded-3xl shadow-2xl p-5 space-y-4">
                
                {/* Mockup Header */}
                <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-xl bg-emerald-600 p-1 flex items-center justify-center">
                      <img src="/logo.svg" alt="MassaGo Icon" className="w-full h-full object-contain" />
                    </div>
                    <div>
                      <div className="text-xs font-bold text-slate-900 dark:text-white">MassaGo Mobile</div>
                      <div className="text-[10px] text-emerald-600 font-medium flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping"></span>
                        Radar Armada Aktif
                      </div>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 text-[10px] font-bold">
                    GPS Live
                  </span>
                </div>

                {/* Live Order Simulation Box */}
                <div className="p-4 rounded-2xl bg-gradient-to-br from-emerald-600 to-teal-700 text-white space-y-3 shadow-md">
                  <div className="flex items-center justify-between text-xs">
                    <span className="bg-white/20 backdrop-blur-sm px-2 py-0.5 rounded-full font-medium text-[11px]">
                      Pesanan Dalam Perjalanan
                    </span>
                    <span className="font-mono font-bold text-amber-300">ETA 12 Menit</span>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="w-11 h-11 rounded-full bg-white/20 border border-white/30 flex items-center justify-center font-bold text-sm">
                      RN
                    </div>
                    <div>
                      <div className="font-bold text-sm">Rina Nurhayati</div>
                      <div className="text-xs text-emerald-100 flex items-center gap-1">
                        <Star className="w-3 h-3 fill-amber-300 text-amber-300" /> 4.98 • 412 Selesai
                      </div>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-white/20 flex items-center justify-between text-xs text-emerald-50">
                    <span>Layanan: Pijat Tradisional Jawa</span>
                    <span className="font-bold">90 Mnt</span>
                  </div>
                </div>

                {/* Popular Services Quick Carousel */}
                <div className="space-y-2">
                  <div className="text-xs font-bold text-slate-700 dark:text-slate-300">Paling Sering Dipesan</div>
                  
                  <div className="p-3 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-xl bg-emerald-100 dark:bg-emerald-950/60 flex items-center justify-center text-emerald-600 font-bold text-xs">
                        💆
                      </div>
                      <div>
                        <div className="text-xs font-bold text-slate-900 dark:text-white">Pijat Tradisional Jawa</div>
                        <div className="text-[10px] text-slate-500">Mulai Rp 140.000 (90 Mnt)</div>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400">Pesan</span>
                  </div>

                  <div className="p-3 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-xl bg-amber-100 dark:bg-amber-950/60 flex items-center justify-center text-amber-600 font-bold text-xs">
                        🦶
                      </div>
                      <div>
                        <div className="text-xs font-bold text-slate-900 dark:text-white">Refleksi Kaki & Relaksasi</div>
                        <div className="text-[10px] text-slate-500">Mulai Rp 110.000 (60 Mnt)</div>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400">Pesan</span>
                  </div>
                </div>

                {/* Mockup Footer CTA */}
                <button
                  onClick={() => {
                    setSelectedApp('customer');
                    setDownloadModalOpen(true);
                  }}
                  className="w-full py-2.5 rounded-xl bg-slate-900 dark:bg-white text-white dark:text-slate-900 font-bold text-xs flex items-center justify-center gap-1.5 shadow-sm hover:opacity-90 transition-opacity"
                >
                  <span>Buka di Aplikasi Android</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* 💆 SERVICE CATALOG SECTION */}
      <section id="layanan" className="py-20 bg-white dark:bg-slate-900 border-t border-slate-200/80 dark:border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          
          <div className="text-center max-w-3xl mx-auto space-y-3 mb-12">
            <span className="text-xs font-bold uppercase tracking-widest text-emerald-600 dark:text-emerald-400">
              KATALOG LAYANAN RESMI
            </span>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Pilihan Treatment Sesuai Kebutuhan Tubuh Anda
            </h2>
            <p className="text-sm sm:text-base text-slate-600 dark:text-slate-300">
              Seluruh paket terstandarisasi dengan durasi penuh, perlengkapan higienis steril, dan minyak aromaterapi pilihan.
            </p>

            {/* Category Filter Tabs */}
            <div className="flex flex-wrap items-center justify-center gap-2 pt-4">
              {[
                { key: 'all', label: 'Semua Layanan' },
                { key: 'tradisional', label: 'Pijat Tradisional' },
                { key: 'refleksi', label: 'Refleksi Kaki' },
                { key: 'spa', label: 'Body Spa & Lulur' },
                { key: 'khusus', label: 'Spesialis & Sport' }
              ].map(cat => (
                <button
                  key={cat.key}
                  onClick={() => setActiveCategory(cat.key as any)}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                    activeCategory === cat.key
                      ? 'bg-emerald-600 text-white shadow-md shadow-emerald-500/20'
                      : 'bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300'
                  }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>
          </div>

          {/* Service Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredServices.map(srv => (
              <div
                key={srv.id}
                className="bg-slate-50 dark:bg-slate-950 border border-slate-200/80 dark:border-slate-800 rounded-3xl p-6 flex flex-col justify-between hover:border-emerald-500/50 dark:hover:border-emerald-500/50 transition-all duration-300 hover:shadow-xl group"
              >
                <div className="space-y-4">
                  {/* Top Badge & Duration */}
                  <div className="flex items-center justify-between">
                    <span className="px-3 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300 uppercase tracking-wider">
                      {srv.badge}
                    </span>
                    <span className="flex items-center gap-1 text-xs font-bold text-slate-500 dark:text-slate-400">
                      <Clock className="w-3.5 h-3.5 text-emerald-500" />
                      {srv.duration}
                    </span>
                  </div>

                  <div>
                    <h3 className="text-xl font-bold text-slate-900 dark:text-white group-hover:text-emerald-600 dark:group-hover:text-emerald-400 transition-colors">
                      {srv.title}
                    </h3>
                    <p className="text-xs text-slate-500 dark:text-slate-400 font-medium mt-0.5">
                      {srv.tagline}
                    </p>
                  </div>

                  <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed font-normal">
                    {srv.description}
                  </p>

                  {/* Feature Checklist */}
                  <div className="space-y-2 pt-2 border-t border-slate-200/60 dark:border-slate-800">
                    {srv.features.map((f, idx) => (
                      <div key={idx} className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-300">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 flex-shrink-0" />
                        <span>{f}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Card Bottom: Price & CTA */}
                <div className="pt-6 mt-6 border-t border-slate-200/80 dark:border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] text-slate-400 uppercase font-semibold block">Mulai Dari</span>
                    <span className="text-lg font-extrabold text-slate-900 dark:text-white">{srv.price}</span>
                  </div>

                  <button
                    onClick={() => {
                      setSelectedApp('customer');
                      setDownloadModalOpen(true);
                    }}
                    className="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-md shadow-emerald-500/20 transition-all flex items-center gap-1.5"
                  >
                    <span>Pesan Layanan</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>

        </div>
      </section>

      {/* 🛡️ WHY CHOOSE US (4 PILARS) */}
      <section id="keunggulan" className="py-20 bg-slate-50 dark:bg-slate-950">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          
          <div className="text-center max-w-3xl mx-auto space-y-3 mb-16">
            <span className="text-xs font-bold uppercase tracking-widest text-emerald-600 dark:text-emerald-400">
              KENAPA MEMILIH PIJATIN
            </span>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Standar Kualitas & Kenyamanan Tertinggi
            </h2>
            <p className="text-sm sm:text-base text-slate-600 dark:text-slate-300">
              Kami tidak hanya sekadar menghubungkan Anda dengan terapis, tapi memastikan setiap sesi pemijatan memenuhi standar higienis dan etika tertinggi.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            
            <div className="p-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl space-y-3 shadow-sm hover:shadow-md transition-shadow">
              <div className="w-12 h-12 rounded-2xl bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold text-xl">
                <Award className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Terapis Tersertifikasi</h3>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                Seluruh mitra melalui verifikasi ketat identitas (KTP, SKCK) dan lulus uji kompetensi anatomi & teknik pijat sebelum turun ke lapangan.
              </p>
            </div>

            <div className="p-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl space-y-3 shadow-sm hover:shadow-md transition-shadow">
              <div className="w-12 h-12 rounded-2xl bg-teal-100 dark:bg-teal-950/60 text-teal-600 dark:text-teal-400 flex items-center justify-center font-bold text-xl">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Higienitas Terjamin</h3>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                Mitra dilengkapi matras portable, kain sprei steril sekali pakai (disposable sheet), dan minyak aromaterapi resmi bersertifikasi BPOM.
              </p>
            </div>

            <div className="p-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl space-y-3 shadow-sm hover:shadow-md transition-shadow">
              <div className="w-12 h-12 rounded-2xl bg-amber-100 dark:bg-amber-950/60 text-amber-600 dark:text-amber-400 flex items-center justify-center font-bold text-xl">
                <DollarSign className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Tarif Pasti & Transparan</h3>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                Harga yang Anda lihat di aplikasi adalah harga final. Tidak ada biaya tersembunyi, ongkos bensin tambahan, maupun pemerasan di tempat.
              </p>
            </div>

            <div className="p-6 bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-3xl space-y-3 shadow-sm hover:shadow-md transition-shadow">
              <div className="w-12 h-12 rounded-2xl bg-rose-100 dark:bg-rose-950/60 text-rose-600 dark:text-rose-400 flex items-center justify-center font-bold text-xl">
                <HeartHandshake className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Keamanan & Tombol SOS</h3>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                Aplikasi dilengkapi fitur Panic Button SOS 24 jam dan kebijakan Anti-Pelecehan ketat untuk menjaga keamanan Anda dan keluarga.
              </p>
            </div>

          </div>

        </div>
      </section>

      {/* 🧭 HOW IT WORKS (3 STEPS) */}
      <section id="cara-kerja" className="py-20 bg-white dark:bg-slate-900 border-t border-slate-200/80 dark:border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          
          <div className="text-center max-w-3xl mx-auto space-y-3 mb-16">
            <span className="text-xs font-bold uppercase tracking-widest text-emerald-600 dark:text-emerald-400">
              ALUR PEMESANAN
            </span>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              3 Langkah Praktis Menikmati Pijat
            </h2>
            <p className="text-sm sm:text-base text-slate-600 dark:text-slate-300">
              Pesan terapis semudah memesan makanan online, siap meluncur kapan pun Anda butuh.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
            
            {/* Step 1 */}
            <div className="p-8 bg-slate-50 dark:bg-slate-950 border border-slate-200/80 dark:border-slate-800 rounded-3xl space-y-4 text-center relative">
              <div className="w-14 h-14 rounded-2xl bg-emerald-600 text-white font-black text-xl mx-auto flex items-center justify-center shadow-lg shadow-emerald-600/20">
                1
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Pilih Paket & Waktu</h3>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                Buka aplikasi MassaGo, pilih jenis treatment pijat yang diinginkan, dan tentukan apakah ingin dipijat sekarang atau dijadwalkan.
              </p>
            </div>

            {/* Step 2 */}
            <div className="p-8 bg-slate-50 dark:bg-slate-950 border border-slate-200/80 dark:border-slate-800 rounded-3xl space-y-4 text-center relative">
              <div className="w-14 h-14 rounded-2xl bg-emerald-600 text-white font-black text-xl mx-auto flex items-center justify-center shadow-lg shadow-emerald-600/20">
                2
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Terapis Tiba di Lokasi</h3>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                Sistem otomatis mencocokkan terapis terdekat yang langsung meluncur ke rumah Anda membawa seluruh perlengkapan steril.
              </p>
            </div>

            {/* Step 3 */}
            <div className="p-8 bg-slate-50 dark:bg-slate-950 border border-slate-200/80 dark:border-slate-800 rounded-3xl space-y-4 text-center relative">
              <div className="w-14 h-14 rounded-2xl bg-emerald-600 text-white font-black text-xl mx-auto flex items-center justify-center shadow-lg shadow-emerald-600/20">
                3
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">Relaksasi & Bayar Mudah</h3>
              <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
                Nikmati pemijatan relaksasi berkualitas, selesaikan pembayaran via Tunai atau QRIS, lalu beri ulasan bintang untuk terapis Anda.
              </p>
            </div>

          </div>

        </div>
      </section>

      {/* 🤝 JOIN AS THERAPIST MITRA SECTION */}
      <section id="mitra" className="py-20 bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 text-white relative overflow-hidden">
        
        {/* Background glow */}
        <div className="absolute right-0 top-1/2 -translate-y-1/2 w-96 h-96 bg-emerald-500/10 blur-[100px] rounded-full pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            
            <div className="lg:col-span-7 space-y-6">
              <span className="px-3.5 py-1 rounded-full text-xs font-bold bg-amber-400/20 text-amber-300 border border-amber-400/30 uppercase tracking-widest">
                PELUANG KARIR MITRA TERAPIS
              </span>

              <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight leading-tight">
                Punya Keahlian Memijat? <br />
                <span className="text-emerald-400">Raih Penghasilan Lebih Tinggi Bersama MassaGo.</span>
              </h2>

              <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
                Bergabunglah dengan ribuan mitra terapis profesional di seluruh Indonesia. Dapatkan kemudahan mengatur jam kerja sendiri dan penghasilan tanpa batas dengan sistem bagi hasil paling adil.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-1">
                  <div className="text-emerald-400 font-bold text-lg flex items-center gap-1.5">
                    <TrendingUp className="w-5 h-5" /> Bagi Hasil 80%
                  </div>
                  <p className="text-xs text-slate-300">Pendapatan terbesar langsung milik Anda tanpa potongan tersembunyi.</p>
                </div>

                <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-1">
                  <div className="text-emerald-400 font-bold text-lg flex items-center gap-1.5">
                    <Clock className="w-5 h-5" /> Jam Fleksibel
                  </div>
                  <p className="text-xs text-slate-300">Bebas tentukan waktu online & istirahat sesuai kesiapan Anda.</p>
                </div>

                <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-1">
                  <div className="text-emerald-400 font-bold text-lg flex items-center gap-1.5">
                    <ShieldCheck className="w-5 h-5" /> Jaminan Keamanan
                  </div>
                  <p className="text-xs text-slate-300">Konsumen terverifikasi NIK & tombol darurat SOS aktif 24 jam.</p>
                </div>

                <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-1">
                  <div className="text-emerald-400 font-bold text-lg flex items-center gap-1.5">
                    <Zap className="w-5 h-5" /> Pencairan Cepat
                  </div>
                  <p className="text-xs text-slate-300">Penarikan dana saldo hasil kerja instan kapan pun dibutuhkan.</p>
                </div>
              </div>

              <div className="pt-4 flex flex-col sm:flex-row gap-4">
                <button
                  onClick={() => {
                    setSelectedApp('mitra');
                    setDownloadModalOpen(true);
                  }}
                  className="px-8 py-4 rounded-2xl bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold text-sm shadow-xl shadow-emerald-500/25 transition-all flex items-center justify-center gap-2"
                >
                  <Download className="w-4 h-4" />
                  <span>Download Aplikasi MassaGo Mitra</span>
                </button>

                <a
                  href="https://wa.me/6281298765432?text=Halo%20Admin%20MassaGo,%20saya%20terapis%20dan%20ingin%20mendaftar%20menjadi%20mitra."
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-6 py-4 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/20 text-white font-semibold text-sm transition-all flex items-center justify-center gap-2"
                >
                  <MessageCircle className="w-4 h-4 text-emerald-400" />
                  <span>Daftar via WhatsApp</span>
                </a>
              </div>
            </div>

            <div className="lg:col-span-5 flex justify-center">
              <div className="p-6 rounded-3xl bg-white/10 backdrop-blur-md border border-white/20 max-w-sm w-full space-y-4 text-center">
                <div className="w-16 h-16 rounded-2xl bg-emerald-500 p-2 mx-auto shadow-lg shadow-emerald-500/30">
                  <img src="/logo.svg" alt="MassaGo Mitra" className="w-full h-full object-contain" />
                </div>
                <h3 className="text-xl font-bold">MassaGo Mitra App</h3>
                <p className="text-xs text-slate-300">
                  Aplikasi khusus terapis untuk menerima orderan, navigasi rute GPS ke rumah klien, dan cek saldo harian.
                </p>
                <div className="pt-2 text-xs text-emerald-300 font-semibold">
                  Versi 1.0.0 • Siap Install di Android
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* ❓ FREQUENTLY ASKED QUESTIONS (FAQ) */}
      <section id="faq" className="py-20 bg-slate-50 dark:bg-slate-950">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          
          <div className="text-center space-y-3 mb-12">
            <span className="text-xs font-bold uppercase tracking-widest text-emerald-600 dark:text-emerald-400">
              PUSAT INFORMASI
            </span>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Pertanyaan yang Sering Diajukan
            </h2>
            <p className="text-sm text-slate-600 dark:text-slate-300">
              Punya pertanyaan seputar layanan dan keamanan MassaGo? Temukan jawabannya di bawah ini.
            </p>
          </div>

          <div className="space-y-4">
            {faqs.map((f, idx) => (
              <div
                key={idx}
                className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm transition-all"
              >
                <button
                  onClick={() => setFaqOpen(faqOpen === idx ? null : idx)}
                  className="w-full p-5 text-left font-bold text-sm sm:text-base text-slate-900 dark:text-white flex items-center justify-between gap-4 focus:outline-none"
                >
                  <span>{f.q}</span>
                  <ChevronDown
                    className={`w-5 h-5 text-slate-400 transition-transform duration-200 flex-shrink-0 ${
                      faqOpen === idx ? 'rotate-180 text-emerald-600' : ''
                    }`}
                  />
                </button>

                {faqOpen === idx && (
                  <div className="px-5 pb-5 text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed border-t border-slate-100 dark:border-slate-800/60 pt-3 animate-fadeIn">
                    {f.a}
                  </div>
                )}
              </div>
            ))}
          </div>

        </div>
      </section>

      {/* 📥 DOWNLOAD APPS BANNER */}
      <section className="py-16 bg-emerald-600 text-white relative overflow-hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6 relative z-10">
          <h2 className="text-3xl sm:text-4xl font-black tracking-tight">
            Rasakan Kenyamanan Pijat Profesional Sekarang Juga
          </h2>
          <p className="text-emerald-100 text-sm sm:text-base max-w-2xl mx-auto">
            Unduh aplikasi resmi MassaGo di smartphone Android Anda dan nikmati promo diskon spesial pengguna baru.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
            <a
              href="/apk/MassaGo-Customer.apk"
              download="MassaGo-Customer.apk"
              className="px-6 py-3.5 rounded-2xl bg-white text-emerald-950 font-bold text-xs shadow-lg hover:bg-slate-100 transition-all flex items-center gap-2"
            >
              <Smartphone className="w-4 h-4 text-emerald-600" />
              <span>Download APK Pelanggan (26 MB)</span>
            </a>

            <a
              href="/apk/MassaGo-Mitra.apk"
              download="MassaGo-Mitra.apk"
              className="px-6 py-3.5 rounded-2xl bg-emerald-900/40 hover:bg-emerald-900/60 border border-emerald-400/40 text-white font-bold text-xs shadow-lg transition-all flex items-center gap-2"
            >
              <Smartphone className="w-4 h-4 text-amber-400" />
              <span>Download APK Mitra Terapis (20 MB)</span>
            </a>
          </div>
        </div>
      </section>

      {/* 📄 FOOTER */}
      <footer className="bg-slate-950 text-slate-400 text-xs py-14 border-t border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-10">
          
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            {/* Col 1: Brand Info */}
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-600 p-1 flex items-center justify-center">
                  <img src="/logo.svg" alt="MassaGo" className="w-full h-full object-contain" />
                </div>
                <span className="text-xl font-bold text-white tracking-tight">MassaGo</span>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                Platform pemesanan layanan pijat & spa panggilan profesional ke rumah, apartemen, dan hotel. Higienis, terpercaya, dan aman untuk keluarga.
              </p>
              <div className="text-[11px] text-emerald-400 font-semibold">
                🛡️ Strictly Professional Health & Wellness Service
              </div>
            </div>

            {/* Col 2: Layanan */}
            <div className="space-y-3">
              <h4 className="font-bold text-white uppercase tracking-wider text-xs">Katalog Layanan</h4>
              <ul className="space-y-2">
                <li><a href="#layanan" className="hover:text-emerald-400">Pijat Tradisional Jawa</a></li>
                <li><a href="#layanan" className="hover:text-emerald-400">Refleksi Kaki & Akupresur</a></li>
                <li><a href="#layanan" className="hover:text-emerald-400">Deep Tissue & Sport Massage</a></li>
                <li><a href="#layanan" className="hover:text-emerald-400">Body Scrub & Lulur Spa</a></li>
                <li><a href="#layanan" className="hover:text-emerald-400">Pijat Ibu Hamil</a></li>
              </ul>
            </div>

            {/* Col 3: Area Layanan & Karir */}
            <div className="space-y-3">
              <h4 className="font-bold text-white uppercase tracking-wider text-xs">Jangkauan & Mitra</h4>
              <ul className="space-y-2">
                <li><span>Jakarta, Bogor, Depok, Tangerang, Bekasi</span></li>
                <li><span>Bandung, Surabaya, Yogyakarta, Bali</span></li>
                <li><a href="#mitra" className="hover:text-emerald-400 font-semibold text-emerald-400">Pendaftaran Mitra Terapis</a></li>
                <li><Link href="/admin" className="hover:text-emerald-400 font-semibold text-emerald-400">Portal Superadmin Web</Link></li>
              </ul>
            </div>

            {/* Col 4: Kontak CS */}
            <div className="space-y-3">
              <h4 className="font-bold text-white uppercase tracking-wider text-xs">Bantuan & Kontak</h4>
              <p className="text-xs text-slate-400">
                Layanan bantuan pelanggan 24/7 dan penanganan darurat:
              </p>
              <div className="space-y-1.5 font-medium text-slate-300">
                <div>WhatsApp CS: +62 812-9876-5432</div>
                <div>Email: support@massago.id</div>
                <div>Operational Hours: 07:00 - 23:00 WIB</div>
              </div>
            </div>
          </div>

          <div className="pt-8 border-t border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-4 text-[11px] text-slate-500">
            <div>
              &copy; {new Date().getFullYear()} PT MassaGo Sehat Indonesia. All rights reserved.
            </div>
            <div className="flex items-center gap-6">
              <a href="#faq" className="hover:text-slate-400">Syarat & Ketentuan</a>
              <a href="#faq" className="hover:text-slate-400">Kebijakan Privasi</a>
              <Link href="/admin" className="hover:text-emerald-400 font-semibold">Portal Superadmin</Link>
            </div>
          </div>

        </div>
      </footer>

      {/* 📱 DOWNLOAD MODAL POPUP */}
      {downloadModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl relative space-y-6">
            
            <button
              onClick={() => setDownloadModalOpen(false)}
              className="absolute top-4 right-4 p-2 rounded-full text-slate-400 hover:text-slate-600 dark:hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Modal Header */}
            <div className="text-center space-y-2">
              <div className="w-14 h-14 rounded-2xl bg-emerald-600 p-1.5 mx-auto flex items-center justify-center shadow-lg shadow-emerald-500/20">
                <img src="/logo.svg" alt="MassaGo" className="w-full h-full object-contain" />
              </div>
              <h3 className="text-xl font-bold text-slate-900 dark:text-white">
                Download Aplikasi MassaGo
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Pilih aplikasi yang ingin Anda unduh langsung ke smartphone Android:
              </p>
            </div>

            {/* App Selection Tabs */}
            <div className="grid grid-cols-2 gap-2 p-1 bg-slate-100 dark:bg-slate-800 rounded-xl">
              <button
                onClick={() => setSelectedApp('customer')}
                className={`py-2 text-xs font-bold rounded-lg transition-all ${
                  selectedApp === 'customer'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-300'
                }`}
              >
                Pelanggan
              </button>
              <button
                onClick={() => setSelectedApp('mitra')}
                className={`py-2 text-xs font-bold rounded-lg transition-all ${
                  selectedApp === 'mitra'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-300'
                }`}
              >
                Mitra Terapis
              </button>
            </div>

            {/* Download Action Box */}
            <div className="p-4 bg-slate-50 dark:bg-slate-800/60 rounded-2xl border border-slate-200/80 dark:border-slate-700/60 space-y-3 text-center">
              {selectedApp === 'customer' ? (
                <>
                  <div className="text-sm font-bold text-slate-900 dark:text-white">MassaGo Customer v1.0.0</div>
                  <div className="text-xs text-slate-500">Ukuran file: ~26 MB • Android 8.0+</div>
                  <a
                    href="/apk/MassaGo-Customer.apk"
                    download="MassaGo-Customer.apk"
                    className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-md shadow-emerald-500/20"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download APK Pelanggan</span>
                  </a>
                </>
              ) : (
                <>
                  <div className="text-sm font-bold text-slate-900 dark:text-white">MassaGo Mitra Terapis v1.0.0</div>
                  <div className="text-xs text-slate-500">Ukuran file: ~20 MB • Android 8.0+</div>
                  <a
                    href="/apk/MassaGo-Mitra.apk"
                    download="MassaGo-Mitra.apk"
                    className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-md shadow-emerald-500/20"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download APK Mitra Terapis</span>
                  </a>
                </>
              )}
            </div>

            <div className="text-[11px] text-slate-400 text-center leading-relaxed">
              💡 Buka file <code>.apk</code> yang terunduh di HP Anda, lalu pilih <em>&ldquo;Install&rdquo;</em> (Izinkan install dari browser jika diminta).
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
