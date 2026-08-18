'use client';

import React from 'react';
import {
  Bell,
  RefreshCw,
  Search,
  ShieldAlert,
  SlidersHorizontal,
  User,
  Sun,
  Moon,
} from 'lucide-react';
import { ActiveTab } from '../../types';

interface HeaderProps {
  activeTab: ActiveTab;
  onRefresh: () => void;
  isRefreshing: boolean;
  activeSosCount: number;
  onSosClick: () => void;
  isDarkMode: boolean;
  onToggleTheme: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  onRefresh,
  isRefreshing,
  activeSosCount,
  onSosClick,
  isDarkMode,
  onToggleTheme,
}) => {
  const tabTitles: Record<ActiveTab, { title: string; subtitle: string }> = {
    dashboard: {
      title: 'Executive Dashboard & Overview',
      subtitle: 'Monitoring metrik performa bisnis, GMV, dan operasional platform realtime',
    },
    godview: {
      title: 'Live Fleet & Dispatch God-View',
      subtitle: 'Peta posisi seluruh mitra terapis dan pesanan yang sedang berjalan di jalan raya',
    },
    orders: {
      title: 'Pusat Manajemen Pesanan',
      subtitle: 'Pantau seluruh alur status order, dispatch terapis, dan intervensi pembatalan/selesai',
    },
    mitra: {
      title: 'Manajemen Mitra Terapis',
      subtitle: 'Kontrol status tugas, verifikasi, pengaturan tier, dan deposit saldo mitra',
    },
    customers: {
      title: 'Database & Manajemen Pelanggan',
      subtitle: 'Pantau riwayat pemesanan pelanggan, saldo dompet, review, dan loyalitas customer',
    },
    kyc: {
      title: 'Pusat Verifikasi KYC Mitra Baru',
      subtitle: 'Review berkas pendaftaran mitra: KTP, SKCK, dan Sertifikasi Pijat BNSP',
    },
    catalog: {
      title: 'Manajemen Katalog Layanan & Tarif',
      subtitle: 'Pengaturan paket pijat, durasi 60/90/120 menit (sinkron otomatis ke aplikasi customer)',
    },
    vouchers: {
      title: 'Voucher Promo & Diskon Marketing',
      subtitle: 'Kelola kode diskon promo, minimal belanja, dan batasan maksimal potongan',
    },
    sos: {
      title: 'Pusat Tanggap Darurat (Emergency SOS)',
      subtitle: 'Log insiden bahaya dan peringatan darurat yang dipicu oleh mitra atau pelanggan',
    },
    finance: {
      title: 'Buku Kas & Pembagian Komisi 20%',
      subtitle: 'Rekapitulasi total GMV, potongan komisi platform, dan pencairan saldo mitra',
    },
    payments: {
      title: 'Pengaturan Rekening & Metode Pembayaran',
      subtitle: 'Kelola rekening bank transfer manual, QRIS platform, komisi, dan kontak konfirmasi WhatsApp',
    },
  };

  const current = tabTitles[activeTab] || { title: 'PijatIn Admin', subtitle: 'Command Center' };

  return (
    <header className="h-16 px-6 bg-white/90 dark:bg-slate-900/80 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 flex items-center justify-between flex-shrink-0 z-20 transition-colors duration-200">
      {/* Title & Subtitle */}
      <div>
        <h1 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">{current.title}</h1>
        <p className="text-xs text-slate-500 dark:text-slate-400 hidden md:block">{current.subtitle}</p>
      </div>

      {/* Right Action Tools */}
      <div className="flex items-center gap-3">
        {/* Active SOS Critical Alert Banner */}
        {activeSosCount > 0 && (
          <button
            onClick={onSosClick}
            className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold animate-pulse shadow-lg shadow-rose-600/30 transition"
          >
            <ShieldAlert className="w-4 h-4" />
            <span>{activeSosCount} Panggilan SOS Aktif!</span>
          </button>
        )}

        {/* Theme Switcher Toggle (Default: Light Mode) */}
        <button
          onClick={onToggleTheme}
          className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-slate-700/60 transition flex items-center gap-1.5 text-xs font-medium"
          title={isDarkMode ? 'Beralih ke Mode Terang (Light Mode)' : 'Beralih ke Mode Gelap (Dark Mode)'}
        >
          {isDarkMode ? (
            <>
              <Sun className="w-4 h-4 text-amber-400" />
              <span className="hidden sm:inline">Light</span>
            </>
          ) : (
            <>
              <Moon className="w-4 h-4 text-slate-600" />
              <span className="hidden sm:inline">Dark</span>
            </>
          )}
        </button>

        {/* Refresh Sync Button */}
        <button
          onClick={onRefresh}
          disabled={isRefreshing}
          className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-slate-700/60 transition disabled:opacity-50"
          title="Segarkan Data"
        >
          <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-emerald-600 dark:text-emerald-400' : ''}`} />
        </button>

        {/* Superadmin Profile Pill */}
        <div className="flex items-center gap-2.5 pl-3 border-l border-slate-200 dark:border-slate-800">
          <div className="w-8 h-8 rounded-xl bg-emerald-100 dark:bg-emerald-600/20 border border-emerald-300 dark:border-emerald-500/30 flex items-center justify-center text-emerald-700 dark:text-emerald-400 font-bold text-xs">
            SA
          </div>
          <div className="hidden lg:block text-left">
            <div className="text-xs font-semibold text-slate-900 dark:text-white">Superadmin Pusat</div>
            <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-medium">Headquarters DIY</div>
          </div>
        </div>
      </div>
    </header>
  );
};
