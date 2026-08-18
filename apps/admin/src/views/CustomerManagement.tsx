'use client';

import React, { useState } from 'react';
import {
  Users,
  Search,
  MessageCircle,
  Calendar,
  ShieldCheck,
  Smartphone,
  ExternalLink,
  UserCheck,
  Clock,
  Sparkles,
  ShoppingBag,
  MapPin,
  CreditCard,
  CheckCircle2,
  TrendingUp,
  X,
  PhoneCall,
  HeartHandshake,
} from 'lucide-react';
import { CustomerProfile, Order, Therapist } from '../types';

interface CustomerManagementProps {
  customers: CustomerProfile[];
  orders: Order[];
  therapists?: Therapist[];
  onRefresh: () => void;
}

export const CustomerManagement: React.FC<CustomerManagementProps> = ({
  customers,
  orders,
  therapists = [],
  onRefresh,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerProfile | null>(null);

  // Filter customers by search term
  const filteredCustomers = customers.filter((c) => {
    const term = searchTerm.toLowerCase();
    return (
      (c.full_name && c.full_name.toLowerCase().includes(term)) ||
      (c.phone && c.phone.toLowerCase().includes(term)) ||
      (c.id && c.id.toLowerCase().includes(term))
    );
  });

  // Calculate global stats
  const totalCustomers = customers.length;
  const activeOrdersCount = orders.filter((o) => o.status !== 'CANCELLED' && o.status !== 'COMPLETED_PAYMENT').length;

  const getCustomerOrders = (customerId: string, phone: string) => {
    const cleanPhone = (phone || '').replace(/\D/g, '');
    return orders.filter(
      (o) =>
        (customerId && o.customer_id === customerId) ||
        (cleanPhone && o.customer_phone && o.customer_phone.replace(/\D/g, '').includes(cleanPhone))
    );
  };

  const formatWhatsAppNumber = (phone: string) => {
    let clean = (phone || '').replace(/\D/g, '');
    if (clean.startsWith('0')) clean = '62' + clean.substring(1);
    if (!clean.startsWith('62')) clean = '62' + clean;
    return clean;
  };

  const getTherapistInfo = (therapistId?: string) => {
    if (!therapistId) return null;
    return therapists.find((t) => t.id === therapistId) || null;
  };

  const getCustomerStats = (cust: CustomerProfile) => {
    const custOrders = getCustomerOrders(cust.id, cust.phone);
    const completedOrders = custOrders.filter((o) => o.status === 'COMPLETED_PAYMENT' || o.status === 'COMPLETED');
    const totalSpent = completedOrders.reduce((sum, o) => sum + (o.total_price || 0), 0);

    // Find favorite service
    const serviceCounts: Record<string, number> = {};
    custOrders.forEach((o) => {
      if (o.service_name) {
        serviceCounts[o.service_name] = (serviceCounts[o.service_name] || 0) + 1;
      }
    });
    let favService = '-';
    let maxCount = 0;
    Object.entries(serviceCounts).forEach(([name, count]) => {
      if (count > maxCount) {
        maxCount = count;
        favService = name;
      }
    });

    return {
      totalOrders: custOrders.length,
      completedOrdersCount: completedOrders.length,
      totalSpent,
      favService,
    };
  };

  return (
    <div className="space-y-6">
      {/* Header & Refresh */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 dark:text-white tracking-tight flex items-center gap-2.5">
            <Users className="w-6 h-6 text-emerald-600 dark:text-emerald-400" />
            Manajemen Data Pelanggan
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Daftar lengkap akun pelanggan, verifikasi WhatsApp, riwayat pesanan, serta detail terapis yang menangani.
          </p>
        </div>

        <button
          onClick={onRefresh}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-bold rounded-xl bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-750 transition shadow-sm"
        >
          <Sparkles className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
          Perbarui Data
        </button>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">Total Pelanggan Terdaftar</span>
            <div className="w-8 h-8 rounded-xl bg-emerald-50 dark:bg-emerald-500/10 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
              <UserCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2 text-2xl font-extrabold text-slate-900 dark:text-white">{totalCustomers}</div>
          <p className="text-[11px] text-emerald-600 dark:text-emerald-400 mt-1 font-medium flex items-center gap-1">
            <ShieldCheck className="w-3.5 h-3.5" />
            Terverifikasi WhatsApp OTP & Password SHA-256
          </p>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">Pesanan Sedang Berjalan</span>
            <div className="w-8 h-8 rounded-xl bg-sky-50 dark:bg-sky-500/10 flex items-center justify-center text-sky-600 dark:text-sky-400">
              <ShoppingBag className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2 text-2xl font-extrabold text-slate-900 dark:text-white">{activeOrdersCount}</div>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 font-medium">
            Dalam penanganan terapis & penjemputan
          </p>
        </div>

        <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">Keamanan & Otoritas Sistem</span>
            <div className="w-8 h-8 rounded-xl bg-amber-50 dark:bg-amber-500/10 flex items-center justify-center text-amber-600 dark:text-amber-400">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-2 text-2xl font-extrabold text-slate-900 dark:text-white">Admin Master</div>
          <p className="text-[11px] text-emerald-600 dark:text-emerald-400 mt-1 font-medium">
            Kendali penuh fleet & otentikasi
          </p>
        </div>
      </div>

      {/* Customer List Card */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm overflow-hidden">
        {/* Search Toolbar */}
        <div className="p-4 border-b border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Cari nama, nomor WhatsApp, atau ID pelanggan..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-9 pr-4 py-2 text-xs bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 dark:text-white"
            />
          </div>

          <div className="text-xs font-medium text-slate-500 dark:text-slate-400">
            Menampilkan <span className="font-bold text-slate-900 dark:text-white">{filteredCustomers.length}</span> dari {totalCustomers} pelanggan
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400 border-b border-slate-200 dark:border-slate-800 uppercase text-[10px] font-bold tracking-wider">
              <tr>
                <th className="py-3 px-4">Pelanggan</th>
                <th className="py-3 px-4">Kontak WhatsApp</th>
                <th className="py-3 px-4">Status Akun</th>
                <th className="py-3 px-4">Terdaftar</th>
                <th className="py-3 px-4">Riwayat Belanja</th>
                <th className="py-3 px-4 text-right">Aksi</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/80">
              {filteredCustomers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400 text-xs">
                    <Users className="w-10 h-10 mx-auto mb-2 text-slate-300 dark:text-slate-600" />
                    Tidak ada data pelanggan yang sesuai dengan pencarian.
                  </td>
                </tr>
              ) : (
                filteredCustomers.map((cust) => {
                  const stats = getCustomerStats(cust);
                  const waNumber = formatWhatsAppNumber(cust.phone || '');
                  const initials = (cust.full_name || 'Pelanggan')
                    .split(' ')
                    .filter(Boolean)
                    .map((n) => n[0])
                    .slice(0, 2)
                    .join('')
                    .toUpperCase() || 'P';

                  return (
                    <tr
                      key={cust.id}
                      className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition group cursor-pointer"
                      onClick={() => setSelectedCustomer(cust)}
                    >
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white font-bold text-xs flex items-center justify-center shadow-sm">
                            {initials}
                          </div>
                          <div>
                            <div className="font-bold text-slate-900 dark:text-white">
                              {cust.full_name || 'Pelanggan MassaGo'}
                            </div>
                            <div className="text-[10px] text-slate-400 font-mono">
                              ID: {cust.id ? cust.id.substring(0, 8) : 'CUST'}...
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-2">
                          <Smartphone className="w-3.5 h-3.5 text-slate-400" />
                          <span className="font-semibold text-slate-800 dark:text-slate-200">
                            {cust.phone || '-'}
                          </span>
                        </div>
                      </td>

                      <td className="py-3.5 px-4">
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-400">
                          <ShieldCheck className="w-3 h-3" />
                          Aktif & Terverifikasi
                        </span>
                      </td>

                      <td className="py-3.5 px-4 text-slate-500 dark:text-slate-400 text-[11px]">
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          <span>
                            {cust.created_at
                              ? new Date(cust.created_at).toLocaleDateString('id-ID', {
                                  day: 'numeric',
                                  month: 'short',
                                  year: 'numeric',
                                })
                              : 'Hari ini'}
                          </span>
                        </div>
                      </td>

                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-900 dark:text-white">
                          {stats.totalOrders} Pesanan
                        </div>
                        <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold">
                          Total Rp {stats.totalSpent.toLocaleString('id-ID')}
                        </div>
                      </td>

                      <td className="py-3.5 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center justify-end gap-2">
                          {waNumber && (
                            <a
                              href={`https://wa.me/${waNumber}?text=Halo%20kak%20${encodeURIComponent(
                                cust.full_name || 'Pelanggan'
                              )},%20ada%20yang%20bisa%20kami%20bantu%20dari%20Layanan%20Pelanggan%20MassaGo%3F`}
                              target="_blank"
                              rel="noreferrer"
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-bold bg-emerald-50 text-emerald-700 hover:bg-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-400 dark:hover:bg-emerald-500/20 transition"
                              title="Chat WhatsApp"
                            >
                              <MessageCircle className="w-3.5 h-3.5" />
                              <span>Hubungi WA</span>
                            </a>
                          )}

                          <button
                            onClick={() => setSelectedCustomer(cust)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                            <span>Detail</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Rich Comprehensive Customer Detail Modal */}
      {selectedCustomer && (() => {
        const stats = getCustomerStats(selectedCustomer);
        const custOrders = getCustomerOrders(selectedCustomer.id, selectedCustomer.phone);
        const waNumber = formatWhatsAppNumber(selectedCustomer.phone || '');
        const initials = (selectedCustomer.full_name || 'Pelanggan')
          .split(' ')
          .filter(Boolean)
          .map((n) => n[0])
          .slice(0, 2)
          .join('')
          .toUpperCase() || 'P';

        return (
          <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in overflow-y-auto">
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl w-full max-w-2xl shadow-2xl overflow-hidden animate-in zoom-in-95 my-8">
              {/* Modal Top Header */}
              <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50/70 dark:bg-slate-800/40">
                <div className="flex items-center gap-2">
                  <UserCheck className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
                  <h2 className="text-base font-extrabold text-slate-900 dark:text-white">
                    Detail Akun & Riwayat Pelanggan
                  </h2>
                </div>
                <button
                  onClick={() => setSelectedCustomer(null)}
                  className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="p-6 space-y-6 max-h-[80vh] overflow-y-auto">
                {/* Profile Hero Card */}
                <div className="p-5 bg-gradient-to-br from-emerald-500/10 via-teal-500/5 to-transparent border border-emerald-500/20 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white font-black text-xl flex items-center justify-center shadow-lg shadow-emerald-500/20">
                      {initials}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-lg font-black text-slate-900 dark:text-white">
                          {selectedCustomer.full_name || 'Pelanggan MassaGo'}
                        </h3>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-600 text-white">
                          CUSTOMER
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 dark:text-slate-300 font-semibold mt-0.5 flex items-center gap-1.5">
                        <Smartphone className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                        {selectedCustomer.phone}
                      </p>
                      <p className="text-[11px] text-slate-400 font-mono mt-0.5">
                        ID: {selectedCustomer.id}
                      </p>
                    </div>
                  </div>

                  {waNumber && (
                    <a
                      href={`https://wa.me/${waNumber}?text=Halo%20kak%20${encodeURIComponent(
                        selectedCustomer.full_name || 'Pelanggan'
                      )},%20ada%20yang%20bisa%20kami%20bantu%20dari%20Layanan%20Pelanggan%20MassaGo%3F`}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md shadow-emerald-600/30 transition shrink-0"
                    >
                      <MessageCircle className="w-4 h-4" />
                      <span>Chat WhatsApp</span>
                    </a>
                  )}
                </div>

                {/* Customer Metric Stats Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800 rounded-xl">
                    <span className="text-[10px] font-bold text-slate-400 uppercase block">Total Belanja</span>
                    <span className="text-sm font-extrabold text-emerald-600 dark:text-emerald-400 mt-1 block">
                      Rp {stats.totalSpent.toLocaleString('id-ID')}
                    </span>
                  </div>

                  <div className="p-3.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800 rounded-xl">
                    <span className="text-[10px] font-bold text-slate-400 uppercase block">Total Pesanan</span>
                    <span className="text-sm font-extrabold text-slate-900 dark:text-white mt-1 block">
                      {stats.totalOrders} Kali
                    </span>
                  </div>

                  <div className="p-3.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800 rounded-xl">
                    <span className="text-[10px] font-bold text-slate-400 uppercase block">Order Selesai</span>
                    <span className="text-sm font-extrabold text-sky-600 dark:text-sky-400 mt-1 block">
                      {stats.completedOrdersCount} Pesanan
                    </span>
                  </div>

                  <div className="p-3.5 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800 rounded-xl">
                    <span className="text-[10px] font-bold text-slate-400 uppercase block">Layanan Favorit</span>
                    <span className="text-xs font-bold text-slate-800 dark:text-slate-200 mt-1 block truncate">
                      {stats.favService}
                    </span>
                  </div>
                </div>

                {/* Orders History with Therapist Handling Info */}
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-extrabold text-slate-900 dark:text-white uppercase tracking-wider flex items-center gap-1.5">
                      <ShoppingBag className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                      Riwayat Pesanan & Terapis ({custOrders.length})
                    </h4>
                    <span className="text-[11px] text-slate-400">Diurutkan dari yang terbaru</span>
                  </div>

                  {custOrders.length === 0 ? (
                    <div className="text-center py-8 bg-slate-50 dark:bg-slate-800/40 rounded-2xl border border-dashed border-slate-200 dark:border-slate-800 text-xs text-slate-400">
                      Pelanggan ini belum melakukan pemesanan layanan.
                    </div>
                  ) : (
                    <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
                      {custOrders.map((ord) => {
                        const therapist = getTherapistInfo(ord.therapist_id) || ord.therapist;
                        const isCompleted = ord.status === 'COMPLETED_PAYMENT' || ord.status === 'COMPLETED';

                        return (
                          <div
                            key={ord.id}
                            className="p-4 bg-white dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700/80 rounded-2xl space-y-3 shadow-sm hover:border-emerald-500/40 transition"
                          >
                            {/* Order Header: Service, Status, Total */}
                            <div className="flex items-start justify-between gap-2 border-b border-slate-100 dark:border-slate-700/60 pb-2.5">
                              <div>
                                <div className="flex items-center gap-2">
                                  <span className="font-extrabold text-sm text-slate-900 dark:text-white">
                                    {ord.service_name}
                                  </span>
                                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300">
                                    {ord.duration_minutes} Menit
                                  </span>
                                </div>
                                <div className="text-[10px] text-slate-400 mt-0.5">
                                  ID: {ord.id} •{' '}
                                  {ord.created_at
                                    ? new Date(ord.created_at).toLocaleDateString('id-ID', {
                                        day: 'numeric',
                                        month: 'short',
                                        year: 'numeric',
                                        hour: '2-digit',
                                        minute: '2-digit',
                                      })
                                    : '-'}
                                </div>
                              </div>

                              <div className="text-right">
                                <div className="font-black text-sm text-emerald-600 dark:text-emerald-400">
                                  Rp {(ord.total_price || 0).toLocaleString('id-ID')}
                                </div>
                                <span
                                  className={`inline-block text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase ${
                                    isCompleted
                                      ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-400'
                                      : ord.status === 'CANCELLED'
                                      ? 'bg-rose-100 text-rose-800 dark:bg-rose-500/20 dark:text-rose-400'
                                      : 'bg-amber-100 text-amber-800 dark:bg-amber-500/20 dark:text-amber-400'
                                  }`}
                                >
                                  {ord.status}
                                </span>
                              </div>
                            </div>

                            {/* Therapist Handling Details */}
                            <div className="p-3 bg-slate-50 dark:bg-slate-900/60 rounded-xl border border-slate-100 dark:border-slate-800 flex items-center justify-between gap-3 text-xs">
                              <div className="flex items-center gap-2.5">
                                <div className="w-8 h-8 rounded-lg bg-teal-600 text-white font-black text-xs flex items-center justify-center">
                                  {therapist?.name ? therapist.name[0].toUpperCase() : 'T'}
                                </div>
                                <div>
                                  <div className="text-[10px] text-slate-400 font-semibold uppercase">
                                    Terapis yang Menangani
                                  </div>
                                  <div className="font-bold text-slate-900 dark:text-white">
                                    {therapist?.name || ord.therapist_id || 'Terapis Siaga MassaGo'}
                                  </div>
                                </div>
                              </div>

                              {therapist?.phone && (
                                <a
                                  href={`https://wa.me/${formatWhatsAppNumber(therapist.phone)}`}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[10px] font-bold bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 transition"
                                >
                                  <PhoneCall className="w-3 h-3 text-emerald-600" />
                                  <span>{therapist.phone}</span>
                                </a>
                              )}
                            </div>

                            {/* Order Address & Notes */}
                            {ord.address && (
                              <div className="flex items-start gap-2 text-[11px] text-slate-500 dark:text-slate-400">
                                <MapPin className="w-3.5 h-3.5 text-rose-500 mt-0.5 shrink-0" />
                                <span className="line-clamp-2">{ord.address}</span>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>

              {/* Modal Footer */}
              <div className="p-4 bg-slate-50 dark:bg-slate-800/50 border-t border-slate-200 dark:border-slate-800 flex justify-end">
                <button
                  onClick={() => setSelectedCustomer(null)}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md transition"
                >
                  Tutup Informasi
                </button>
              </div>
            </div>
          </div>
        );
      })()}
    </div>
  );
};
