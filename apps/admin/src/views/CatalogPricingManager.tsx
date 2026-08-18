'use client';

import React, { useState } from 'react';
import {
  Sparkles,
  Plus,
  Edit3,
  Trash2,
  CheckCircle2,
  Clock,
  Tag,
  DollarSign,
  Layers,
  Save,
} from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { ServicePackage } from '../types';
import { upsertServicePackage, deleteServicePackage } from '../lib/supabase';

interface CatalogPricingManagerProps {
  services: ServicePackage[];
  onRefresh: () => void;
}

export const CatalogPricingManager: React.FC<CatalogPricingManagerProps> = ({
  services,
  onRefresh,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingService, setEditingService] = useState<Partial<ServicePackage> | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const formatRupiah = (num: number) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(num);
  };

  const handleOpenAddModal = () => {
    setEditingService({
      id: `SRV-${Math.floor(1000 + Math.random() * 9000)}`,
      name: '',
      category: 'Tradisional',
      icon_emoji: '💆‍♂️',
      short_description: '',
      full_description: '',
      benefits: ['Melancarkan peredaran darah', 'Meredakan otot kaku'],
      price_60: 120000,
      price_90: 160000,
      price_120: 210000,
      orders_count: 0,
    });
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (service: ServicePackage) => {
    setEditingService({ ...service });
    setIsModalOpen(true);
  };

  const handleSave = async () => {
    if (!editingService || !editingService.name || !editingService.id) return;
    setIsSaving(true);

    const success = await upsertServicePackage(editingService);
    setIsSaving(false);

    if (success) {
      setIsModalOpen(false);
      setEditingService(null);
      onRefresh();
    } else {
      alert('Gagal menyimpan paket layanan ke Supabase. Pastikan tabel service_packages sudah dibuat di Supabase SQL Editor.');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Apakah Anda yakin ingin menghapus paket layanan ini dari katalog?')) return;
    const success = await deleteServicePackage(id);
    if (success) {
      onRefresh();
    } else {
      alert('Gagal menghapus paket layanan dari Supabase.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner & Add Service Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-sm">
        <div>
          <h2 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">
            Katalog Paket Layanan & Pengaturan Tarif
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Semua perubahan harga dan paket pijat akan otomatis tersinkronisasi langsung ke aplikasi customer.
          </p>
        </div>

        <button
          onClick={handleOpenAddModal}
          className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md shadow-emerald-600/30 transition transform hover:-translate-y-0.5"
        >
          <Plus className="w-4 h-4" />
          <span>Tambah Paket Pijat Baru</span>
        </button>
      </div>

      {/* Services Grid Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {services.map((service) => (
          <div
            key={service.id}
            className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 hover:border-slate-300 dark:hover:border-slate-700 transition shadow-sm flex flex-col justify-between"
          >
            <div className="space-y-3">
              {/* Header: Icon & Category */}
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-2xl bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700/80 flex items-center justify-center text-2xl shadow-inner">
                    {service.icon_emoji}
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white leading-snug">{service.name}</h3>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-[10px] font-mono text-slate-500 dark:text-slate-400">{service.id}</span>
                      <span className="text-slate-300 dark:text-slate-600">•</span>
                      <span className="text-[10px] font-semibold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 px-1.5 py-0.5 rounded">
                        {service.category}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Descriptions */}
              <p className="text-xs text-slate-600 dark:text-slate-400 line-clamp-2 leading-relaxed">
                {service.short_description || service.full_description}
              </p>

              {/* Tiered Durations & Pricing Grid */}
              <div className="grid grid-cols-3 gap-1.5 pt-2 border-t border-slate-100 dark:border-slate-800 text-center">
                <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80">
                  <div className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">60 Mnt</div>
                  <div className="text-xs font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">
                    {service.price_60 > 0 ? formatRupiah(service.price_60) : '-'}
                  </div>
                </div>

                <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80">
                  <div className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">90 Mnt</div>
                  <div className="text-xs font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">
                    {service.price_90 > 0 ? formatRupiah(service.price_90) : '-'}
                  </div>
                </div>

                <div className="p-2 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800/80">
                  <div className="text-[10px] text-slate-500 dark:text-slate-400 font-medium">120 Mnt</div>
                  <div className="text-xs font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">
                    {service.price_120 > 0 ? formatRupiah(service.price_120) : '-'}
                  </div>
                </div>
              </div>

              {/* Benefits Checklist */}
              <div className="space-y-1 pt-1">
                {(service.benefits || []).slice(0, 3).map((b, idx) => (
                  <div key={idx} className="flex items-center gap-1.5 text-[11px] text-slate-700 dark:text-slate-300">
                    <span className="text-emerald-600 dark:text-emerald-400">✓</span>
                    <span className="truncate">{b}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Bottom Actions & Orders count */}
            <div className="flex items-center justify-between pt-4 mt-4 border-t border-slate-100 dark:border-slate-800">
              <span className="text-[11px] text-slate-500 font-medium">
                ⭐ {service.orders_count || 0} order tuntas
              </span>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleOpenEditModal(service)}
                  className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-slate-700/60 transition"
                  title="Edit Paket Layanan"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={() => handleDelete(service.id)}
                  className="p-2 rounded-xl bg-rose-50 hover:bg-rose-100 dark:bg-rose-500/10 dark:hover:bg-rose-500/20 text-rose-700 dark:text-rose-400 border border-rose-200 dark:border-rose-500/20 transition"
                  title="Hapus Layanan"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Add / Edit Service Modal */}
      {editingService && (
        <Modal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          title={editingService.name ? `Edit Paket: ${editingService.name}` : 'Tambah Paket Pijat Baru'}
          subtitle="Data akan langsung tersimpan di Supabase dan dapat dipesan pelanggan"
        >
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-3">
              <div className="col-span-2">
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Nama Layanan:</label>
                <input
                  type="text"
                  placeholder="Contoh: Pijat Refleksi & Akupresur"
                  value={editingService.name || ''}
                  onChange={(e) => setEditingService({ ...editingService, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Ikon Emoji:</label>
                <input
                  type="text"
                  placeholder="💆‍♂️ / 🦶 / ✨"
                  value={editingService.icon_emoji || '💆‍♂️'}
                  onChange={(e) => setEditingService({ ...editingService, icon_emoji: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500 text-center"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Kategori:</label>
                <select
                  value={editingService.category || 'Tradisional'}
                  onChange={(e) => setEditingService({ ...editingService, category: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                >
                  <option value="Tradisional">Tradisional</option>
                  <option value="Refleksi">Refleksi</option>
                  <option value="Kebugaran">Kebugaran</option>
                  <option value="Spa & Kulit">Spa & Kulit</option>
                  <option value="Kesehatan">Kesehatan</option>
                  <option value="Khusus">Khusus</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Kode ID Layanan:</label>
                <input
                  type="text"
                  value={editingService.id || ''}
                  onChange={(e) => setEditingService({ ...editingService, id: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-500 dark:text-slate-400 text-xs rounded-xl border border-slate-200 dark:border-slate-700 font-mono"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Deskripsi Singkat:</label>
              <textarea
                rows={2}
                placeholder="Penjelasan ringkas manfaat pijat..."
                value={editingService.short_description || ''}
                onChange={(e) => setEditingService({ ...editingService, short_description: e.target.value })}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
            </div>

            {/* Pricing Matrix */}
            <div className="bg-slate-50 dark:bg-slate-950/60 p-4 rounded-xl border border-slate-200 dark:border-slate-800 space-y-3">
              <span className="text-xs font-bold text-slate-900 dark:text-white block">Tarif Harga Berdasarkan Durasi:</span>

              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="text-[11px] text-slate-500 dark:text-slate-400 block mb-1">Tarif 60 Mnt (Rp):</label>
                  <input
                    type="number"
                    value={editingService.price_60 || 0}
                    onChange={(e) =>
                      setEditingService({ ...editingService, price_60: parseInt(e.target.value, 10) || 0 })
                    }
                    className="w-full px-3 py-1.5 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-lg border border-slate-200 dark:border-slate-700 font-mono"
                  />
                </div>

                <div>
                  <label className="text-[11px] text-slate-500 dark:text-slate-400 block mb-1">Tarif 90 Mnt (Rp):</label>
                  <input
                    type="number"
                    value={editingService.price_90 || 0}
                    onChange={(e) =>
                      setEditingService({ ...editingService, price_90: parseInt(e.target.value, 10) || 0 })
                    }
                    className="w-full px-3 py-1.5 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-lg border border-slate-200 dark:border-slate-700 font-mono"
                  />
                </div>

                <div>
                  <label className="text-[11px] text-slate-500 dark:text-slate-400 block mb-1">Tarif 120 Mnt (Rp):</label>
                  <input
                    type="number"
                    value={editingService.price_120 || 0}
                    onChange={(e) =>
                      setEditingService({ ...editingService, price_120: parseInt(e.target.value, 10) || 0 })
                    }
                    className="w-full px-3 py-1.5 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-lg border border-slate-200 dark:border-slate-700 font-mono"
                  />
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-2">
              <button
                onClick={handleSave}
                disabled={isSaving || !editingService.name}
                className="w-full py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md shadow-emerald-600/30 transition disabled:opacity-50 flex items-center justify-center gap-2"
              >
                <Save className="w-4 h-4" />
                <span>Simpan Paket ke Database Cloud</span>
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
