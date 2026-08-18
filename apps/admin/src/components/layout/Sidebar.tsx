'use client';

import React from 'react';
import {
  LayoutDashboard,
  MapPin,
  ClipboardList,
  UserCheck,
  Users,
  ShieldCheck,
  Sparkles,
  Ticket,
  AlertTriangle,
  Wallet,
  CreditCard,
  Activity,
} from 'lucide-react';
import { ActiveTab } from '../../types';

interface SidebarProps {
  activeTab: ActiveTab;
  setActiveTab: (tab: ActiveTab) => void;
  onlineMitraCount: number;
  activeOrdersCount: number;
  pendingKycCount: number;
  activeSosCount: number;
  customersCount?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  setActiveTab,
  onlineMitraCount,
  activeOrdersCount,
  pendingKycCount,
  activeSosCount,
  customersCount = 0,
}) => {
  const mainMenuItems = [
    {
      id: 'dashboard' as ActiveTab,
      label: 'Dashboard Overview',
      icon: LayoutDashboard,
      badge: null,
    },
    {
      id: 'godview' as ActiveTab,
      label: 'Live Fleet (God-View)',
      icon: MapPin,
      badge: onlineMitraCount > 0 ? `${onlineMitraCount} Online` : null,
      badgeColor: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-400',
    },
    {
      id: 'orders' as ActiveTab,
      label: 'Live Orders Dispatch',
      icon: ClipboardList,
      badge: activeOrdersCount > 0 ? `${activeOrdersCount}` : null,
      badgeColor: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400',
    },
    {
      id: 'customers' as ActiveTab,
      label: 'Data Pelanggan',
      icon: UserCheck,
      badge: customersCount > 0 ? `${customersCount}` : null,
      badgeColor: 'bg-teal-100 text-teal-700 dark:bg-teal-500/20 dark:text-teal-400',
    },
    {
      id: 'mitra' as ActiveTab,
      label: 'Mitra & Verifikasi KYC',
      icon: Users,
      badge: pendingKycCount > 0 ? `${pendingKycCount} KYC` : null,
      badgeColor: 'bg-amber-500 text-white font-bold',
    },
  ];

  const serviceMenuItems = [
    {
      id: 'catalog' as ActiveTab,
      label: 'Katalog Layanan Pijat',
      icon: Sparkles,
      badge: null,
    },
    {
      id: 'vouchers' as ActiveTab,
      label: 'Voucher & Promo',
      icon: Ticket,
      badge: null,
    },
  ];

  const systemMenuItems = [
    {
      id: 'finance' as ActiveTab,
      label: 'Keuangan & Payout',
      icon: Wallet,
      badge: null,
    },
    {
      id: 'payments' as ActiveTab,
      label: 'Pengaturan Pembayaran',
      icon: CreditCard,
      badge: null,
    },
    {
      id: 'sos' as ActiveTab,
      label: 'Emergency SOS Center',
      icon: AlertTriangle,
      badge: activeSosCount > 0 ? `${activeSosCount} Alert` : null,
      badgeColor: 'bg-rose-500 text-white animate-pulse',
    },
  ];

  const renderNavGroup = (title: string, items: typeof mainMenuItems) => (
    <div className="space-y-1 mb-4">
      <div className="px-3 pb-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
        {title}
      </div>
      {items.map((item) => {
        const Icon = item.icon;
        const isActive = activeTab === item.id;

        return (
          <button
            key={item.id}
            onClick={() => setActiveTab(item.id)}
            className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-semibold transition ${
              isActive
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800/60'
            }`}
          >
            <div className="flex items-center gap-2.5 min-w-0">
              <Icon className={`w-4 h-4 flex-shrink-0 ${isActive ? 'text-white' : 'text-slate-400'}`} />
              <span className="truncate">{item.label}</span>
            </div>

            {item.badge && (
              <span
                className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex-shrink-0 ${
                  isActive ? 'bg-white/20 text-white' : item.badgeColor
                }`}
              >
                {item.badge}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );

  return (
    <aside className="w-64 bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-800 flex flex-col flex-shrink-0 select-none h-screen transition-colors duration-200">
      {/* Brand Header */}
      <div className="h-16 px-5 flex items-center gap-3 border-b border-slate-200 dark:border-slate-800 bg-white/80 dark:bg-slate-900/80">
        <img src="/logo.svg" alt="PijatIn Logo" className="w-9 h-9 object-contain rounded-xl shadow-sm shadow-emerald-600/20" />
        <div>
          <div className="flex items-center gap-1.5">
            <span className="font-extrabold text-slate-900 dark:text-white text-base tracking-tight">PijatIn</span>
            <span className="text-[10px] font-bold bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 px-1.5 py-0.5 rounded uppercase">
              Admin
            </span>
          </div>
          <p className="text-[10px] text-slate-500 dark:text-slate-400">Command Center</p>
        </div>
      </div>

      {/* Navigation List */}
      <nav className="flex-1 px-3 py-4 overflow-y-auto">
        {renderNavGroup('Operasional Utama', mainMenuItems)}
        {renderNavGroup('Layanan & Promo', serviceMenuItems)}
        {renderNavGroup('Sistem & Keuangan', systemMenuItems)}
      </nav>

      {/* Status Live Feed Indicator */}
      <div className="p-4 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-950/40">
        <div className="flex items-center gap-2 mb-2">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
          </span>
          <span className="text-[11px] font-medium text-emerald-600 dark:text-emerald-400">Supabase Cloud Connected</span>
        </div>
        <div className="text-[11px] text-slate-500 dark:text-slate-400 flex items-center justify-between">
          <span>Server Response</span>
          <span className="font-mono text-slate-700 dark:text-slate-300">~24 ms</span>
        </div>
      </div>
    </aside>
  );
};
