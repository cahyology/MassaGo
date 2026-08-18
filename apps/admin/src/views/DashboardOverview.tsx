'use client';

import React from 'react';
import {
  TrendingUp,
  DollarSign,
  Users,
  CheckCircle2,
  Clock,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  AlertTriangle,
  ChevronRight,
  Activity,
  Award,
} from 'lucide-react';
import { StatCard } from '../components/common/StatCard';
import { Badge } from '../components/common/Badge';
import { Therapist, Order, ServicePackage, ActiveTab } from '../types';
import { cleanAddressText } from '../components/map/LiveFleetMap';

interface DashboardOverviewProps {
  therapists: Therapist[];
  orders: Order[];
  services: ServicePackage[];
  setActiveTab: (tab: ActiveTab) => void;
}

export const DashboardOverview: React.FC<DashboardOverviewProps> = ({
  therapists,
  orders,
  services,
  setActiveTab,
}) => {
  // Financial & Operational Computations from Real Supabase Data
  const totalGmv = orders.reduce((acc, curr) => acc + (curr.total_price || 0), 0);
  const platformFee = Math.round(totalGmv * 0.2); // 20% platform commission
  const onlineMitra = therapists.filter((t) => t.is_online || t.duty_status === 'ONLINE').length;
  const activeOrders = orders.filter(
    (o) =>
      o.status === 'PENDING' ||
      o.status.startsWith('ACCEPT') ||
      o.status === 'ARRIVED_AT_LOCATION' ||
      o.status === 'TREATMENT_IN_PROGRESS' ||
      o.status === 'IN_SERVICE'
  ).length;
  const completedOrders = orders.filter(
    (o) => o.status.startsWith('COMPLETE') || o.status === 'REVIEW_SUBMITTED'
  ).length;

  const formatRupiah = (num: number) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(num);
  };

  return (
    <div className="space-y-6">
      {/* Top Banner: Real-Time Operational Pulse */}
      <div className="relative overflow-hidden bg-gradient-to-r from-emerald-600 via-emerald-700 to-teal-800 dark:from-emerald-950/80 dark:via-slate-900 dark:to-slate-900 border border-emerald-500/30 rounded-2xl p-6 shadow-lg text-white">
        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full bg-white/20 dark:bg-emerald-500/20 text-white dark:text-emerald-400 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 backdrop-blur-sm">
                <span className="h-2 w-2 rounded-full bg-white dark:bg-emerald-400 animate-pulse" />
                Live Dispatch Center
              </span>
              <span className="text-xs text-emerald-100 dark:text-slate-400">• Daerah Istimewa Yogyakarta & Sekitarnya</span>
            </div>
            <h2 className="text-xl font-bold tracking-tight text-white">
              Platform PijatIn Beroperasi Normal & Optimal
            </h2>
            <p className="text-xs text-emerald-100 dark:text-slate-300 max-w-xl">
              {onlineMitra} Mitra Terapis siap menerima panggilan di area operasional dengan status sinkronisasi realtime cloud.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setActiveTab('godview')}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-emerald-800 hover:bg-emerald-50 dark:bg-emerald-600 dark:hover:bg-emerald-500 dark:text-white text-xs font-bold shadow-lg transition transform hover:-translate-y-0.5"
            >
              <span>Pantau Peta God-View</span>
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={() => setActiveTab('orders')}
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-800/60 hover:bg-emerald-800 text-white dark:bg-slate-800 dark:hover:bg-slate-700 text-xs font-semibold border border-emerald-400/30 dark:border-slate-700 transition"
            >
              <span>Kelola Pesanan ({activeOrders})</span>
            </button>
          </div>
        </div>
      </div>

      {/* Primary KPI Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Gross Merchandise Value (GMV)"
          value={formatRupiah(totalGmv)}
          change="Akumulasi Live"
          isPositive={true}
          icon={TrendingUp}
          iconColor="text-emerald-600 dark:text-emerald-400"
          iconBg="bg-emerald-50 dark:bg-emerald-500/10"
        />

        <StatCard
          title="Komisi Platform (20% Net)"
          value={formatRupiah(platformFee)}
          change="Bagi Hasil 20%"
          isPositive={true}
          icon={DollarSign}
          iconColor="text-sky-600 dark:text-sky-400"
          iconBg="bg-sky-50 dark:bg-sky-500/10"
        />

        <StatCard
          title="Mitra Online & Siap Tugas"
          value={`${onlineMitra} / ${therapists.length}`}
          change={`${Math.round((onlineMitra / Math.max(1, therapists.length)) * 100)}% Siap`}
          isPositive={true}
          icon={Users}
          iconColor="text-amber-600 dark:text-amber-400"
          iconBg="bg-amber-50 dark:bg-amber-500/10"
        />

        <StatCard
          title="Pesanan Berhasil Tuntas"
          value={completedOrders.toString()}
          change="Terselesaikan"
          isPositive={true}
          icon={CheckCircle2}
          iconColor="text-purple-600 dark:text-purple-400"
          iconBg="bg-purple-50 dark:bg-purple-500/10"
        />
      </div>

      {/* Grid 2 Columns: Live Orders Table & Popular Service Packages */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Live Orders Feed */}
        <div className="lg:col-span-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white">Aktivitas Pesanan Realtime</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Daftar order real-time dari database Supabase</p>
            </div>
            <button
              onClick={() => setActiveTab('orders')}
              className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:underline flex items-center gap-1 transition"
            >
              Lihat Semua Order <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800 bg-slate-50/80 dark:bg-slate-950/40">
                <tr>
                  <th className="py-2.5 px-3 font-semibold">Order ID</th>
                  <th className="py-2.5 px-3 font-semibold">Pelanggan & Lokasi</th>
                  <th className="py-2.5 px-3 font-semibold">Layanan</th>
                  <th className="py-2.5 px-3 font-semibold">Total Tarif</th>
                  <th className="py-2.5 px-3 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                {orders.slice(0, 6).map((order) => {
                  const statusMap: Record<string, { label: string; variant: 'emerald' | 'amber' | 'blue' | 'rose' | 'slate' }> = {
                    PENDING: { label: 'Mencari Terapis', variant: 'amber' },
                    ACCEPTED_ON_THE_WAY: { label: 'Menuju Lokasi', variant: 'blue' },
                    ARRIVED_AT_LOCATION: { label: 'Tiba di Lokasi', variant: 'blue' },
                    TREATMENT_IN_PROGRESS: { label: 'Sesi Pijat Berjalan', variant: 'emerald' },
                    COMPLETED_PAYMENT: { label: 'Selesai', variant: 'emerald' },
                    REVIEW_SUBMITTED: { label: 'Selesai & Dinilai', variant: 'emerald' },
                    CANCELLED: { label: 'Dibatalkan', variant: 'rose' },
                  };

                  const currentStatus = statusMap[order.status] || {
                    label: order.status,
                    variant: 'slate',
                  };

                  return (
                    <tr key={order.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition">
                      <td className="py-3 px-3 font-mono font-medium text-slate-700 dark:text-slate-300">
                        {order.id}
                      </td>
                      <td className="py-3 px-3">
                        <div className="font-semibold text-slate-900 dark:text-white">
                          {order.customer_name || 'Pelanggan PijatIn'}
                        </div>
                        <div className="text-[11px] text-slate-500 dark:text-slate-400 truncate max-w-[180px]">
                          {cleanAddressText(order.address).address}
                        </div>
                      </td>
                      <td className="py-3 px-3">
                        <div className="font-medium text-slate-800 dark:text-slate-200">{order.service_name}</div>
                        <div className="text-[11px] text-slate-500 dark:text-slate-400">{order.duration_minutes} Menit</div>
                      </td>
                      <td className="py-3 px-3 font-semibold text-emerald-600 dark:text-emerald-400">
                        {formatRupiah(order.total_price)}
                      </td>
                      <td className="py-3 px-3">
                        <Badge variant={currentStatus.variant} size="sm">
                          {currentStatus.label}
                        </Badge>
                      </td>
                    </tr>
                  );
                })}

                {orders.length === 0 && (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-400">
                      Belum ada transaksi pesanan di database.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right 1 Col: Top Service Catalogs */}
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white">Katalog Paling Diminati</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Paket pijat live dari database</p>
            </div>
            <button
              onClick={() => setActiveTab('catalog')}
              className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 hover:underline flex items-center gap-1 transition"
            >
              Kelola <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="space-y-2.5">
            {services.slice(0, 5).map((service) => (
              <div
                key={service.id}
                className="flex items-center justify-between p-3 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80 hover:border-slate-300 dark:hover:border-slate-700 transition"
              >
                <div className="flex items-center gap-3">
                  <div className="text-xl p-2 rounded-lg bg-white dark:bg-slate-800/80 shadow-sm">
                    {service.icon_emoji}
                  </div>
                  <div>
                    <div className="text-xs font-bold text-slate-900 dark:text-white">{service.name}</div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400">
                      Mulai {formatRupiah(service.price_60 || service.price_90)}
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400">
                    {service.orders_count || 0}
                  </span>
                  <div className="text-[10px] text-slate-400">pesanan</div>
                </div>
              </div>
            ))}
          </div>

          {/* Quick Platform Action Pill */}
          <div className="pt-2 border-t border-slate-100 dark:border-slate-800">
            <button
              onClick={() => setActiveTab('vouchers')}
              className="w-full py-2.5 px-3 rounded-xl bg-emerald-50 hover:bg-emerald-100 dark:bg-emerald-500/10 dark:hover:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 text-xs font-bold flex items-center justify-center gap-2 transition border border-emerald-200 dark:border-emerald-500/20 shadow-sm"
            >
              <Sparkles className="w-4 h-4" />
              <span>Buat Kode Promo / Voucher Baru</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
