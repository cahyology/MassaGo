'use client';

import React, { useState } from 'react';
import {
  ClipboardList,
  Search,
  Filter,
  Eye,
  CheckCircle2,
  XCircle,
  Clock,
  UserCheck,
  Phone,
  MapPin,
  Sparkles,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { Order, Therapist } from '../types';
import { updateOrderStatus } from '../lib/supabase';
import { cleanAddressText } from '../components/map/LiveFleetMap';

interface OrdersCenterProps {
  orders: Order[];
  therapists: Therapist[];
  onRefresh: () => void;
}

export const OrdersCenter: React.FC<OrdersCenterProps> = ({
  orders,
  therapists,
  onRefresh,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [reassignTherapistId, setReassignTherapistId] = useState<string>('');
  const [isUpdating, setIsUpdating] = useState(false);

  const statusCategories = [
    { id: 'ALL', label: 'Semua Pesanan' },
    { id: 'PENDING', label: '🔍 Mencari Terapis' },
    { id: 'ACCEPTED_ON_THE_WAY', label: '🛵 Menuju Lokasi' },
    { id: 'TREATMENT_IN_PROGRESS', label: '💆 Sedang Sesi' },
    { id: 'COMPLETED_PAYMENT', label: '✓ Selesai' },
    { id: 'CANCELLED', label: '✕ Dibatalkan' },
  ];

  const filteredOrders = orders.filter((order) => {
    const matchesSearch =
      order.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (order.customer_name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (order.customer_phone || '').includes(searchQuery) ||
      (order.service_name || '').toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'PENDING') return order.status === 'PENDING';
    if (statusFilter === 'ACCEPTED_ON_THE_WAY')
      return order.status === 'ACCEPTED_ON_THE_WAY' || order.status === 'ARRIVED_AT_LOCATION';
    if (statusFilter === 'TREATMENT_IN_PROGRESS')
      return order.status === 'TREATMENT_IN_PROGRESS' || order.status === 'IN_SERVICE';
    if (statusFilter === 'COMPLETED_PAYMENT')
      return order.status.startsWith('COMPLETE') || order.status === 'REVIEW_SUBMITTED';
    if (statusFilter === 'CANCELLED') return order.status === 'CANCELLED';
    return true;
  });

  const handleStatusChange = async (newStatus: string) => {
    if (!selectedOrder) return;
    setIsUpdating(true);
    const success = await updateOrderStatus(selectedOrder.id, newStatus);
    setIsUpdating(false);
    if (success) {
      setIsDetailModalOpen(false);
      onRefresh();
    }
  };

  const handleReassignTherapist = async () => {
    if (!selectedOrder || !reassignTherapistId) return;
    setIsUpdating(true);
    const success = await updateOrderStatus(
      selectedOrder.id,
      'ACCEPTED_ON_THE_WAY',
      reassignTherapistId
    );
    setIsUpdating(false);
    if (success) {
      setIsDetailModalOpen(false);
      setReassignTherapistId('');
      onRefresh();
    }
  };

  const formatRupiah = (num: number) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(num);
  };

  const formatTimestamp = (ts: number | string) => {
    if (!ts) return '-';
    const date = typeof ts === 'number' ? new Date(ts) : new Date(ts);
    return date.toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' }) + ' WIB';
  };

  return (
    <div className="space-y-6">
      {/* Top Controls: Search & Status Filter Pills */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 flex flex-col md:flex-row items-center justify-between gap-4 shadow-sm">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Cari order ID, pelanggan, layanan..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800/80 text-xs text-slate-900 dark:text-white rounded-xl placeholder:text-slate-400 dark:placeholder:text-slate-500 border border-slate-200 dark:border-slate-700/60 focus:outline-none focus:border-emerald-500"
          />
        </div>

        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {statusCategories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setStatusFilter(cat.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition ${
                statusFilter === cat.id
                  ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30'
                  : 'bg-slate-100 dark:bg-slate-800/80 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-slate-700/60'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800 bg-slate-50/80 dark:bg-slate-950/40 uppercase tracking-wider font-semibold text-[11px]">
              <tr>
                <th className="py-3 px-4">Order ID & Waktu</th>
                <th className="py-3 px-4">Pelanggan & Lokasi</th>
                <th className="py-3 px-4">Layanan & Durasi</th>
                <th className="py-3 px-4">Mitra Terapis</th>
                <th className="py-3 px-4">Total Tarif</th>
                <th className="py-3 px-4">Status Pesanan</th>
                <th className="py-3 px-4 text-right">Rincian & Aksi</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
              {filteredOrders.map((order) => {
                const statusMap: Record<
                  string,
                  { label: string; variant: 'emerald' | 'amber' | 'blue' | 'rose' | 'slate' }
                > = {
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

                const matchedTherapist = therapists.find((t) => t.id === order.therapist_id);
                const { address: cleanAddress, landmark } = cleanAddressText(order.address);

                return (
                  <tr key={order.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition">
                    <td className="py-3.5 px-4">
                      <div className="font-mono font-bold text-slate-900 dark:text-white">{order.id}</div>
                      <div className="text-[11px] text-slate-500 dark:text-slate-400 flex items-center gap-1 mt-0.5">
                        <Clock className="w-3 h-3 text-slate-400" />
                        <span>{formatTimestamp(order.created_at)}</span>
                      </div>
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="font-bold text-slate-900 dark:text-white">{order.customer_name || 'Pelanggan MassaGo'}</div>
                      <div className="text-[11px] text-slate-500 dark:text-slate-400 truncate max-w-[200px] flex items-center gap-1 mt-0.5">
                        <MapPin className="w-3 h-3 text-slate-400 flex-shrink-0" />
                        <span className="truncate">{cleanAddress}</span>
                      </div>
                      {landmark && (
                        <div className="text-[10px] text-emerald-600 dark:text-emerald-400 mt-0.5 truncate max-w-[200px]">
                          Patokan: {landmark}
                        </div>
                      )}
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-slate-800 dark:text-slate-200">{order.service_name}</div>
                      <div className="text-[11px] text-slate-500 dark:text-slate-400">{order.duration_minutes} Menit</div>
                    </td>

                    <td className="py-3.5 px-4">
                      {matchedTherapist ? (
                        <div>
                          <div className="font-semibold text-emerald-600 dark:text-emerald-400">{matchedTherapist.name}</div>
                          <div className="text-[11px] text-slate-500 dark:text-slate-400">{matchedTherapist.phone}</div>
                        </div>
                      ) : order.therapist_id ? (
                        <div className="font-mono text-xs text-slate-700 dark:text-slate-300">{order.therapist_id}</div>
                      ) : (
                        <span className="text-[11px] text-amber-600 dark:text-amber-400 font-medium italic">
                          Menunggu Mitra
                        </span>
                      )}
                    </td>

                    <td className="py-3.5 px-4 font-bold text-emerald-600 dark:text-emerald-400">
                      {formatRupiah(order.total_price)}
                    </td>

                    <td className="py-3.5 px-4">
                      <Badge variant={currentStatus.variant} size="sm">
                        {currentStatus.label}
                      </Badge>
                    </td>

                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => {
                          setSelectedOrder(order);
                          setIsDetailModalOpen(true);
                        }}
                        className="px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold border border-slate-200 dark:border-slate-700 transition"
                      >
                        Detail & Kontrol
                      </button>
                    </td>
                  </tr>
                );
              })}

              {filteredOrders.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    Tidak ada pesanan yang sesuai dengan filter.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Order Detail & Superadmin Dispatch Modal */}
      {selectedOrder && (
        <Modal
          isOpen={isDetailModalOpen}
          onClose={() => setIsDetailModalOpen(false)}
          title={`Detail Pesanan: ${selectedOrder.id}`}
          subtitle={`Dibuat pada: ${formatTimestamp(selectedOrder.created_at)}`}
        >
          <div className="space-y-5">
            {/* Customer & Service Info */}
            <div className="bg-slate-50 dark:bg-slate-950/60 p-4 rounded-xl border border-slate-200 dark:border-slate-800 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500 dark:text-slate-400">Nama Pelanggan:</span>
                <span className="font-bold text-slate-900 dark:text-white">{selectedOrder.customer_name || 'Pelanggan MassaGo'}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500 dark:text-slate-400">Nomor Telepon:</span>
                <span className="font-mono text-slate-700 dark:text-slate-200">{selectedOrder.customer_phone || '-'}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500 dark:text-slate-400">Layanan Dipilih:</span>
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                  {selectedOrder.service_name} ({selectedOrder.duration_minutes} Mnt)
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500 dark:text-slate-400">Total Tarif:</span>
                <span className="font-bold text-slate-900 dark:text-white">{formatRupiah(selectedOrder.total_price)}</span>
              </div>
              <div className="pt-2 border-t border-slate-200 dark:border-slate-800">
                <span className="text-slate-500 dark:text-slate-400 block mb-1">Alamat Penjemputan:</span>
                <span className="text-slate-800 dark:text-slate-200 block font-medium">
                  {cleanAddressText(selectedOrder.address).address}
                </span>
                {cleanAddressText(selectedOrder.address).landmark && (
                  <span className="text-amber-600 dark:text-amber-400 block text-[11px] font-medium mt-0.5">
                    Patokan: {cleanAddressText(selectedOrder.address).landmark}
                  </span>
                )}
              </div>
            </div>

            {/* Reassign / Manual Dispatch Dropdown */}
            <div className="bg-slate-50 dark:bg-slate-950/60 p-4 rounded-xl border border-slate-200 dark:border-slate-800 space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Ganti / Dispatch Mitra Terapis:
                </label>
                {selectedOrder.therapist_id && (
                  <span className="text-[11px] text-emerald-600 dark:text-emerald-400">
                    Mitra Terikat: {selectedOrder.therapist_id}
                  </span>
                )}
              </div>

              <div className="flex gap-2">
                <select
                  value={reassignTherapistId}
                  onChange={(e) => setReassignTherapistId(e.target.value)}
                  className="flex-1 px-3 py-2 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-white rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                >
                  <option value="">-- Pilih Mitra Terapis Online --</option>
                  {therapists
                    .filter((t) => t.is_online)
                    .map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name} ({t.gender}) • ⭐ {t.rating}
                      </option>
                    ))}
                </select>

                <button
                  onClick={handleReassignTherapist}
                  disabled={!reassignTherapistId || isUpdating}
                  className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition disabled:opacity-50"
                >
                  Dispatch
                </button>
              </div>
            </div>

            {/* Manual Status Override Buttons */}
            <div>
              <label className="block text-xs font-semibold text-slate-500 dark:text-slate-400 mb-2">
                Aksi Intervensi Superadmin:
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => handleStatusChange('CANCELLED')}
                  disabled={isUpdating}
                  className="py-2.5 px-3 rounded-xl bg-rose-50 hover:bg-rose-100 dark:bg-rose-500/10 dark:hover:bg-rose-500/20 text-rose-700 dark:text-rose-400 text-xs font-bold border border-rose-200 dark:border-rose-500/20 transition disabled:opacity-50"
                >
                  ✕ Batalkan Pesanan (Refund)
                </button>
                <button
                  onClick={() => handleStatusChange('COMPLETED_PAYMENT')}
                  disabled={isUpdating}
                  className="py-2.5 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md transition disabled:opacity-50"
                >
                  ✓ Force Selesaikan Order
                </button>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
