'use client';

import React, { useState, useEffect } from 'react';
import {
  Wallet,
  DollarSign,
  TrendingUp,
  Percent,
  CheckCircle,
  Save,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { StatCard } from '../components/common/StatCard';
import { Badge } from '../components/common/Badge';
import { Order, Therapist } from '../types';
import { getPlatformSettings, updatePlatformSetting } from '../lib/supabase';

interface FinanceLedgerProps {
  orders: Order[];
  therapists: Therapist[];
}

export const FinanceLedger: React.FC<FinanceLedgerProps> = ({ orders, therapists }) => {
  const [commissionPercent, setCommissionPercent] = useState<number>(20);
  const [isEditingRate, setIsEditingRate] = useState(false);
  const [tempRate, setTempRate] = useState('20');
  const [isSaving, setIsSaving] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3000);
  };

  const loadSettings = async () => {
    const st = await getPlatformSettings();
    const rate = parseInt(st.platform_commission_percent || '20', 10);
    setCommissionPercent(isNaN(rate) ? 20 : rate);
    setTempRate(isNaN(rate) ? '20' : rate.toString());
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const handleSaveCommission = async () => {
    const rateNum = parseInt(tempRate, 10);
    if (isNaN(rateNum) || rateNum < 0 || rateNum > 100) {
      alert('Persentase komisi harus berupa angka antara 0 - 100%');
      return;
    }
    setIsSaving(true);
    const success = await updatePlatformSetting(
      'platform_commission_percent',
      rateNum.toString(),
      'Persentase Komisi Platform'
    );
    setIsSaving(false);
    if (success) {
      setCommissionPercent(rateNum);
      setIsEditingRate(false);
      showToast(`Bagi hasil platform berhasil diubah menjadi ${rateNum}% (Mitra ${100 - rateNum}%)!`);
    } else {
      alert('Gagal menyimpan pengaturan komisi ke database.');
    }
  };

  // Calculate directly from real completed orders
  const totalGmv = orders.reduce((acc, curr) => acc + (curr.total_price || 0), 0);
  const platformRateDecimal = commissionPercent / 100.0;
  const mitraRateDecimal = (100 - commissionPercent) / 100.0;

  const platformFee = Math.round(totalGmv * platformRateDecimal);
  const mitraNetTotal = Math.round(totalGmv * mitraRateDecimal);

  const formatRupiah = (num: number) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(num);
  };

  return (
    <div className="space-y-6">
      {/* Toast Notification */}
      {toastMsg && (
        <div className="fixed top-6 right-6 z-50 bg-slate-900 text-white px-4 py-3 rounded-xl shadow-2xl flex items-center gap-2 border border-emerald-500/30 text-xs font-semibold animate-bounce">
          <CheckCircle className="w-4 h-4 text-emerald-400" />
          <span>{toastMsg}</span>
        </div>
      )}

      {/* Top Banner with Commission Percentage Config */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">
              Buku Kas & Kebijakan Bagi Hasil Platform
            </h2>
            <span className="text-[10px] font-black px-2 py-0.5 bg-emerald-100 dark:bg-emerald-500/20 text-emerald-700 dark:text-emerald-400 rounded-full">
              {commissionPercent}% Platform / {100 - commissionPercent}% Mitra
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Persentase ini otomatis digunakan untuk menghitung potongan komisi transaksi & auto-debit saldo deposit mitra.
          </p>
        </div>

        {/* Quick Edit Commission Form */}
        <div className="flex items-center gap-2 bg-slate-50 dark:bg-slate-800/80 p-2 rounded-xl border border-slate-200 dark:border-slate-700">
          <div className="flex items-center gap-1.5 px-2">
            <Percent className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            <span className="text-xs font-bold text-slate-700 dark:text-slate-300">Potongan Platform:</span>
          </div>

          <div className="relative w-20">
            <input
              type="number"
              min="0"
              max="100"
              value={tempRate}
              onChange={(e) => {
                setTempRate(e.target.value);
                setIsEditingRate(true);
              }}
              className="w-full px-2.5 py-1 text-xs font-black text-slate-900 dark:text-white bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            <span className="absolute right-2 top-1 text-xs font-bold text-slate-400 pointer-events-none">%</span>
          </div>

          {isEditingRate && (
            <button
              onClick={handleSaveCommission}
              disabled={isSaving}
              className="px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition flex items-center gap-1 shadow-sm"
            >
              {isSaving ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Save className="w-3.5 h-3.5" />}
              Simpan
            </button>
          )}
        </div>
      </div>

      {/* 3 Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Total Gross Volume (GMV)"
          value={formatRupiah(totalGmv)}
          change="Akumulasi Order"
          isPositive={true}
          icon={TrendingUp}
          iconColor="text-slate-800 dark:text-white"
          iconBg="bg-slate-100 dark:bg-slate-800"
        />

        <StatCard
          title={`Bagi Hasil Platform (${commissionPercent}% Net)`}
          value={formatRupiah(platformFee)}
          change="Komisi Bersih"
          isPositive={true}
          icon={DollarSign}
          iconColor="text-emerald-600 dark:text-emerald-400"
          iconBg="bg-emerald-50 dark:bg-emerald-500/10"
        />

        <StatCard
          title={`Hak Pendapatan Mitra (${100 - commissionPercent}%)`}
          value={formatRupiah(mitraNetTotal)}
          change="Bagi Hasil Mitra"
          isPositive={true}
          icon={Wallet}
          iconColor="text-sky-600 dark:text-sky-400"
          iconBg="bg-sky-50 dark:bg-sky-500/10"
        />
      </div>

      {/* Commission Breakdown Ledger Table */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-white">
              Buku Besar Transaksi & Split Komisi {commissionPercent}%
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Pembagian bagi hasil otomatis ({commissionPercent}% Platform : {100 - commissionPercent}% Mitra) dari database pesanan realtime
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800 bg-slate-50/80 dark:bg-slate-950/40 uppercase tracking-wider text-[11px] font-semibold">
              <tr>
                <th className="py-2.5 px-3">Order ID</th>
                <th className="py-2.5 px-3">Layanan & Pelanggan</th>
                <th className="py-2.5 px-3">Gross Total</th>
                <th className="py-2.5 px-3">Komisi Platform ({commissionPercent}%)</th>
                <th className="py-2.5 px-3">Pendapatan Mitra ({100 - commissionPercent}%)</th>
                <th className="py-2.5 px-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {orders.map((order) => {
                const price = order.total_price || 0;
                const platformShare = Math.round(price * platformRateDecimal);
                const mitraShare = Math.round(price * mitraRateDecimal);

                return (
                  <tr key={order.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition">
                    <td className="py-3 px-3 font-mono font-semibold text-slate-700 dark:text-slate-300">{order.id}</td>
                    <td className="py-3 px-3">
                      <div className="font-medium text-slate-900 dark:text-white">{order.service_name}</div>
                      <div className="text-[11px] text-slate-500 dark:text-slate-400">{order.customer_name || 'Pelanggan'}</div>
                    </td>
                    <td className="py-3 px-3 font-bold text-slate-900 dark:text-white">{formatRupiah(price)}</td>
                    <td className="py-3 px-3 font-semibold text-emerald-600 dark:text-emerald-400">{formatRupiah(platformShare)}</td>
                    <td className="py-3 px-3 font-semibold text-sky-600 dark:text-sky-400">{formatRupiah(mitraShare)}</td>
                    <td className="py-3 px-3">
                      <Badge variant="emerald" size="sm">
                        {order.status}
                      </Badge>
                    </td>
                  </tr>
                );
              })}

              {orders.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400">
                    Belum ada riwayat transaksi pesanan di database.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
