'use client';

import React, { useState } from 'react';
import {
  Ticket,
  Plus,
  Trash2,
  Percent,
  DollarSign,
  Calendar,
  CheckCircle,
  ToggleLeft,
  ToggleRight,
  Sparkles,
  Save,
} from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { PromoVoucher } from '../types';
import { upsertPromoVoucher, deletePromoVoucher } from '../lib/supabase';

interface VoucherManagerProps {
  vouchers: PromoVoucher[];
  onRefresh: () => void;
}

export const VoucherManager: React.FC<VoucherManagerProps> = ({
  vouchers,
  onRefresh,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState<Partial<PromoVoucher> | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const formatRupiah = (num: number) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(num);
  };

  const handleOpenAddModal = () => {
    setEditingVoucher({
      code: '',
      title: '',
      description: '',
      discount_percent: 30,
      discount_flat: 0,
      max_discount: 35000,
      min_spend: 100000,
      is_active: true,
    });
    setIsModalOpen(true);
  };

  const handleToggleActive = async (voucher: PromoVoucher) => {
    const success = await upsertPromoVoucher({
      ...voucher,
      is_active: !voucher.is_active,
    });
    if (success) onRefresh();
  };

  const handleSave = async () => {
    if (!editingVoucher || !editingVoucher.code || !editingVoucher.title) return;
    setIsSaving(true);
    const success = await upsertPromoVoucher({
      ...editingVoucher,
      code: editingVoucher.code.toUpperCase().trim(),
    });
    setIsSaving(false);
    if (success) {
      setIsModalOpen(false);
      setEditingVoucher(null);
      onRefresh();
    } else {
      alert('Gagal menyimpan voucher ke Supabase. Pastikan tabel promo_vouchers sudah dibuat di Supabase SQL Editor.');
    }
  };

  const handleDelete = async (code: string) => {
    if (!confirm(`Hapus voucher promo ${code}?`)) return;
    const success = await deletePromoVoucher(code);
    if (success) {
      onRefresh();
    } else {
      alert('Gagal menghapus voucher dari Supabase.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
        <div>
          <h2 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">
            Pusat Kode Promo & Voucher Diskon Marketing
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Voucher aktif dapat langsung digunakan pelanggan pada saat checkout di aplikasi customer.
          </p>
        </div>

        <button
          onClick={handleOpenAddModal}
          className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md shadow-emerald-600/30 transition transform hover:-translate-y-0.5"
        >
          <Plus className="w-4 h-4" />
          <span>Buat Kode Promo Baru</span>
        </button>
      </div>

      {/* Vouchers Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {vouchers.map((voucher) => (
          <div
            key={voucher.code}
            className={`bg-white dark:bg-slate-900 border rounded-2xl p-5 transition shadow-sm flex flex-col justify-between ${
              voucher.is_active ? 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700' : 'border-slate-200 dark:border-slate-800/40 opacity-60'
            }`}
          >
            <div className="space-y-3">
              {/* Header: Code & Active Pill */}
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="w-10 h-10 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20 flex items-center justify-center text-amber-600 dark:text-amber-400 font-bold">
                    <Ticket className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-sm font-mono font-extrabold text-slate-900 dark:text-white tracking-wider">
                      {voucher.code}
                    </h3>
                    <div className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                      {voucher.discount_percent > 0
                        ? `Diskon ${voucher.discount_percent}%`
                        : `Potongan ${formatRupiah(voucher.discount_flat)}`}
                    </div>
                  </div>
                </div>

                <Badge variant={voucher.is_active ? 'emerald' : 'slate'} size="sm">
                  {voucher.is_active ? 'Aktif' : 'Non-Aktif'}
                </Badge>
              </div>

              {/* Title & Description */}
              <div>
                <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200">{voucher.title}</h4>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5 line-clamp-2 leading-relaxed">
                  {voucher.description}
                </p>
              </div>

              {/* Terms Matrix */}
              <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100 dark:border-slate-800 text-[11px]">
                <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80">
                  <span className="text-slate-400 block text-[10px]">Min. Transaksi:</span>
                  <span className="font-semibold text-slate-700 dark:text-slate-300 font-mono">
                    {formatRupiah(voucher.min_spend || 100000)}
                  </span>
                </div>
                <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80">
                  <span className="text-slate-400 block text-[10px]">Maks. Potongan:</span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400 font-mono">
                    {formatRupiah(voucher.max_discount || 50000)}
                  </span>
                </div>
              </div>
            </div>

            {/* Bottom Actions */}
            <div className="flex items-center justify-between pt-4 mt-4 border-t border-slate-100 dark:border-slate-800">
              <button
                onClick={() => handleToggleActive(voucher)}
                className={`text-xs font-semibold flex items-center gap-1.5 transition ${
                  voucher.is_active ? 'text-amber-600 dark:text-amber-400 hover:underline' : 'text-emerald-600 dark:text-emerald-400 hover:underline'
                }`}
              >
                {voucher.is_active ? (
                  <>
                    <ToggleRight className="w-4 h-4" /> <span>Nonaktifkan</span>
                  </>
                ) : (
                  <>
                    <ToggleLeft className="w-4 h-4" /> <span>Aktifkan</span>
                  </>
                )}
              </button>

              <button
                onClick={() => handleDelete(voucher.code)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-500/10 transition"
                title="Hapus Voucher"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Add Voucher Modal */}
      {editingVoucher && (
        <Modal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          title="Buat Kode Promo / Voucher Baru"
          subtitle="Kode promo langsung aktif untuk seluruh pelanggan di aplikasi mobile"
        >
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                KODE VOUCHER (Huruf Kapital Tanpa Spasi):
              </label>
              <input
                type="text"
                placeholder="Contoh: PIJATHEMAT, GAJIAN30"
                value={editingVoucher.code || ''}
                onChange={(e) =>
                  setEditingVoucher({ ...editingVoucher, code: e.target.value.toUpperCase() })
                }
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500 font-mono font-bold tracking-wider"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Judul Promo:</label>
              <input
                type="text"
                placeholder="Contoh: Diskon Spesial 30% Akhir Pekan"
                value={editingVoucher.title || ''}
                onChange={(e) => setEditingVoucher({ ...editingVoucher, title: e.target.value })}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Deskripsi Ringkas:</label>
              <textarea
                rows={2}
                placeholder="Ketentuan singkat promo..."
                value={editingVoucher.description || ''}
                onChange={(e) => setEditingVoucher({ ...editingVoucher, description: e.target.value })}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Diskon Persen (%):</label>
                <input
                  type="number"
                  placeholder="0 jika nominal flat"
                  value={editingVoucher.discount_percent || 0}
                  onChange={(e) =>
                    setEditingVoucher({
                      ...editingVoucher,
                      discount_percent: parseInt(e.target.value, 10) || 0,
                    })
                  }
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Diskon Flat (Rp):</label>
                <input
                  type="number"
                  placeholder="0 jika persen"
                  value={editingVoucher.discount_flat || 0}
                  onChange={(e) =>
                    setEditingVoucher({
                      ...editingVoucher,
                      discount_flat: parseInt(e.target.value, 10) || 0,
                    })
                  }
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 font-mono"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Min. Belanja (Rp):</label>
                <input
                  type="number"
                  value={editingVoucher.min_spend || 100000}
                  onChange={(e) =>
                    setEditingVoucher({
                      ...editingVoucher,
                      min_spend: parseInt(e.target.value, 10) || 0,
                    })
                  }
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Maks. Potongan (Rp):</label>
                <input
                  type="number"
                  value={editingVoucher.max_discount || 50000}
                  onChange={(e) =>
                    setEditingVoucher({
                      ...editingVoucher,
                      max_discount: parseInt(e.target.value, 10) || 0,
                    })
                  }
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 font-mono"
                />
              </div>
            </div>

            <div className="pt-3">
              <button
                onClick={handleSave}
                disabled={isSaving || !editingVoucher.code || !editingVoucher.title}
                className="w-full py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md shadow-emerald-600/30 transition disabled:opacity-50 flex items-center justify-center gap-2"
              >
                <Save className="w-4 h-4" />
                <span>Simpan & Rilis Promo Voucher</span>
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
