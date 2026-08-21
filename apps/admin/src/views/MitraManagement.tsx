'use client';

import React, { useState } from 'react';
import {
  Users,
  Search,
  Filter,
  Plus,
  Edit2,
  ShieldCheck,
  Star,
  CheckCircle,
  XCircle,
  Clock,
  Wallet,
  AlertCircle,
  Check,
  X,
  FileText,
  UserCheck,
} from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { Modal } from '../components/common/Modal';
import { Therapist, KycApplication, DutyStatus, Order } from '../types';
import { updateTherapist, supabase } from '../lib/supabase';
import { computeTherapistEffectiveStatus } from '../components/map/LiveFleetMap';

interface MitraManagementProps {
  therapists: Therapist[];
  orders?: Order[];
  initialSubTab?: 'fleet' | 'kyc';
  onRefresh: () => void;
}

export const MitraManagement: React.FC<MitraManagementProps> = ({
  therapists,
  orders = [],
  initialSubTab = 'fleet',
  onRefresh,
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'fleet' | 'kyc'>(initialSubTab);

  React.useEffect(() => {
    if (initialSubTab) {
      setActiveSubTab(initialSubTab);
    }
  }, [initialSubTab]);
  const [searchQuery, setSearchQuery] = useState('');
  const [genderFilter, setGenderFilter] = useState<'ALL' | 'Pria' | 'Wanita'>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ONLINE' | 'OFFLINE' | 'ON_DUTY_BUSY'>('ALL');

  // Modal State for Therapist Editing & Deposit Topup
  const [selectedTherapist, setSelectedTherapist] = useState<Therapist | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [depositAmount, setDepositAmount] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // KYC state - only real registered candidates from database
  const [kycList, setKycList] = useState<KycApplication[]>([]);

  const filteredTherapists = therapists.filter((t) => {
    const matchesSearch =
      t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.phone.includes(searchQuery) ||
      t.id.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;
    if (genderFilter !== 'ALL' && t.gender !== genderFilter) return false;
    const effectiveStatus = computeTherapistEffectiveStatus(t, orders);
    if (statusFilter !== 'ALL' && effectiveStatus !== statusFilter) return false;
    return true;
  });

  const handleUpdateStatus = async (therapist: Therapist, newStatus: DutyStatus) => {
    setIsSubmitting(true);
    const success = await updateTherapist(therapist.id, {
      duty_status: newStatus,
      is_online: newStatus === 'ONLINE',
    });
    setIsSubmitting(false);
    if (success) onRefresh();
  };

  const handleDepositAdjustment = async (isAdd: boolean) => {
    if (!selectedTherapist || !depositAmount) return;
    const amount = parseInt(depositAmount.replace(/\D/g, ''), 10);
    if (isNaN(amount) || amount <= 0) return;

    setIsSubmitting(true);
    const currentDeposit = selectedTherapist.deposit_balance || 0;
    const newDeposit = isAdd ? currentDeposit + amount : Math.max(0, currentDeposit - amount);

    const success = await updateTherapist(selectedTherapist.id, {
      deposit_balance: newDeposit,
    });

    setIsSubmitting(false);
    if (success) {
      setIsEditModalOpen(false);
      setDepositAmount('');
      onRefresh();
    }
  };

  const pendingKycCount = kycList.filter((k) => k.status === 'PENDING').length;

  return (
    <div className="space-y-6">
      {/* Sub-Tabs: Mitra Fleet vs KYC Verification */}
      <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-4">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveSubTab('fleet')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition ${
              activeSubTab === 'fleet'
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30'
                : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-slate-800'
            }`}
          >
            <Users className="w-4 h-4" />
            <span>Daftar Mitra Aktif ({therapists.length})</span>
          </button>

          <button
            onClick={() => setActiveSubTab('kyc')}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition relative ${
              activeSubTab === 'kyc'
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30'
                : 'bg-white dark:bg-slate-900 text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-slate-800'
            }`}
          >
            <ShieldCheck className="w-4 h-4" />
            <span>Pusat Verifikasi KYC</span>
            {pendingKycCount > 0 && (
              <span className="px-1.5 py-0.5 rounded-full bg-rose-500 text-white text-[10px] font-bold">
                {pendingKycCount}
              </span>
            )}
          </button>
        </div>

        {activeSubTab === 'fleet' && (
          <div className="text-xs text-slate-500 dark:text-slate-400">
            Total {filteredTherapists.length} mitra terdaftar di Supabase
          </div>
        )}
      </div>

      {activeSubTab === 'fleet' ? (
        <div className="space-y-4">
          {/* Search & Filter Toolbar */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3 shadow-sm">
            <div className="flex items-center gap-3 flex-1 min-w-[280px]">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Cari nama mitra, telepon, atau ID..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 bg-slate-50 dark:bg-slate-800/80 text-xs text-slate-900 dark:text-white rounded-xl placeholder:text-slate-400 dark:placeholder:text-slate-500 border border-slate-200 dark:border-slate-700/60 focus:outline-none focus:border-emerald-500 transition"
                />
              </div>

              {/* Gender Filter */}
              <select
                value={genderFilter}
                onChange={(e) => setGenderFilter(e.target.value as any)}
                className="px-3 py-2 bg-slate-50 dark:bg-slate-800/80 text-xs text-slate-800 dark:text-slate-200 rounded-xl border border-slate-200 dark:border-slate-700/60 focus:outline-none focus:border-emerald-500"
              >
                <option value="ALL">Semua Gender</option>
                <option value="Pria">Pria</option>
                <option value="Wanita">Wanita</option>
              </select>

              {/* Status Filter */}
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as any)}
                className="px-3 py-2 bg-slate-50 dark:bg-slate-800/80 text-xs text-slate-800 dark:text-slate-200 rounded-xl border border-slate-200 dark:border-slate-700/60 focus:outline-none focus:border-emerald-500"
              >
                <option value="ALL">Semua Status</option>
                <option value="ONLINE">🟢 Online</option>
                <option value="ON_DUTY_BUSY">🟠 Sedang Bertugas</option>
                <option value="OFFLINE">⚪ Offline</option>
              </select>
            </div>
          </div>

          {/* Mitra Table */}
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800 bg-slate-50/80 dark:bg-slate-950/40 uppercase tracking-wider font-semibold text-[11px]">
                  <tr>
                    <th className="py-3 px-4">Terapis & Identitas</th>
                    <th className="py-3 px-4">Kontak & Gender</th>
                    <th className="py-3 px-4">Rating & Jam Terbang</th>
                    <th className="py-3 px-4">Saldo Dompet & Deposit</th>
                    <th className="py-3 px-4">Tier & Radius</th>
                    <th className="py-3 px-4">Status Tugas</th>
                    <th className="py-3 px-4 text-right">Aksi Superadmin</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                  {filteredTherapists.map((therapist) => {
                    const effectiveStatus = computeTherapistEffectiveStatus(therapist, orders);
                    const isBusy = effectiveStatus === 'ON_DUTY_BUSY';
                    const isOnline = effectiveStatus === 'ONLINE';

                    return (
                      <tr key={therapist.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition">
                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-600 to-emerald-400 flex items-center justify-center text-white font-bold text-xs shadow-md">
                              {therapist.name.substring(0, 2).toUpperCase()}
                            </div>
                            <div>
                              <div className="font-bold text-slate-900 dark:text-white text-sm">{therapist.name}</div>
                              <div className="text-[11px] font-mono text-slate-500 dark:text-slate-400">ID: {therapist.id}</div>
                            </div>
                          </div>
                        </td>

                        <td className="py-3.5 px-4">
                          <div className="font-medium text-slate-800 dark:text-slate-200">{therapist.phone}</div>
                          <div className="text-[11px] text-slate-500 dark:text-slate-400">{therapist.gender}</div>
                        </td>

                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-1 font-bold text-amber-500">
                            <Star className="w-3.5 h-3.5 fill-amber-500" />
                            <span>{therapist.rating}</span>
                            <span className="text-[10px] text-slate-400">({therapist.review_count || 0})</span>
                          </div>
                          <div className="text-[11px] text-slate-500 dark:text-slate-400">
                            {therapist.orders_completed || 0} Pesanan Selesai
                          </div>
                        </td>

                        <td className="py-3.5 px-4">
                          <div className="font-semibold text-slate-900 dark:text-white">
                            Rp {Number(therapist.wallet_balance || 0).toLocaleString('id-ID')}
                          </div>
                          <div className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">
                            Deposit: Rp {Number(therapist.deposit_balance || 100000).toLocaleString('id-ID')}
                          </div>
                        </td>

                        <td className="py-3.5 px-4">
                          <span className="inline-block px-2 py-0.5 rounded-md bg-amber-50 dark:bg-amber-500/10 text-amber-700 dark:text-amber-400 text-[10px] font-bold border border-amber-200 dark:border-amber-500/20">
                            {therapist.tier_badge || 'Gold Master'}
                          </span>
                          <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                            Radius: {therapist.max_radius_km || 10} KM
                          </div>
                        </td>

                        <td className="py-3.5 px-4">
                          <Badge
                            variant={isBusy ? 'amber' : isOnline ? 'emerald' : 'slate'}
                            size="sm"
                            pulse={isOnline || isBusy}
                          >
                            {isBusy ? 'Bertugas' : isOnline ? 'Online' : 'Offline'}
                          </Badge>
                        </td>

                        <td className="py-3.5 px-4 text-right">
                          <button
                            onClick={() => {
                              setSelectedTherapist(therapist);
                              setIsEditModalOpen(true);
                            }}
                            className="px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold border border-slate-200 dark:border-slate-700 transition"
                          >
                            Kelola & Saldo
                          </button>
                        </td>
                      </tr>
                    );
                  })}

                  {filteredTherapists.length === 0 && (
                    <tr>
                      <td colSpan={7} className="py-12 text-center text-slate-400">
                        Tidak ada mitra terapis yang sesuai dengan pencarian.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : (
        /* KYC Verification Sub-Tab */
        <div className="space-y-4">
          {therapists.filter((t: any) => t.tier_badge?.includes('Menunggu') || t.tier_badge?.includes('Review') || t.tier_badge === 'Mitra Baru (Menunggu Review)').length === 0 ? (
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-12 text-center shadow-sm">
              <ShieldCheck className="w-12 h-12 text-emerald-500 mx-auto mb-3" />
              <h3 className="text-base font-bold text-slate-900 dark:text-white">Semua Calon Mitra Telah Terverifikasi</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-md mx-auto">
                Saat ini belum ada berkas pendaftaran mitra baru yang menunggu persetujuan (KYC). Calon mitra yang mendaftar via aplikasi Mitra akan langsung muncul di sini untuk Anda setujui.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {therapists
                .filter((t: any) => t.tier_badge?.includes('Menunggu') || t.tier_badge?.includes('Review') || t.tier_badge === 'Mitra Baru (Menunggu Review)')
                .map((candidate: any) => {
                  const certs: string[] = candidate.certifications || [];
                  const nik = certs.find((c) => c.startsWith('NIK:'))?.replace('NIK:', '').trim() || candidate.nik || '-';
                  const bankInfo = certs.find((c) => c.startsWith('Rekening:'))?.replace('Rekening:', '').trim() || candidate.bank_name || 'BCA';
                  const specialties = certs.filter((c) => !c.startsWith('NIK:') && !c.startsWith('Rekening:') && !c.startsWith('pwd:'));

                  return (
                    <div
                      key={candidate.id}
                      className="bg-white dark:bg-slate-900 border border-amber-200 dark:border-amber-500/30 rounded-2xl p-5 shadow-sm space-y-4 relative overflow-hidden"
                    >
                      <div className="absolute top-0 right-0 bg-amber-500 text-white text-[10px] font-extrabold px-3 py-1 rounded-bl-xl uppercase tracking-wider">
                        Menunggu Verifikasi
                      </div>

                      <div className="flex items-start gap-3 pt-1">
                        <div className="w-12 h-12 rounded-2xl bg-amber-100 dark:bg-amber-500/20 border border-amber-300 dark:border-amber-500/30 flex items-center justify-center text-amber-700 dark:text-amber-300 font-extrabold text-base">
                          {candidate.name ? candidate.name.substring(0, 2).toUpperCase() : 'TR'}
                        </div>
                        <div className="flex-1 pr-14">
                          <h4 className="text-sm font-bold text-slate-900 dark:text-white">{candidate.name}</h4>
                          <p className="text-xs text-slate-500 dark:text-slate-400">📱 {candidate.phone}</p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">👤 {candidate.gender} • NIK: {nik}</p>
                        </div>
                      </div>

                      <div className="bg-slate-50 dark:bg-slate-950/60 p-3 rounded-xl border border-slate-200 dark:border-slate-800 space-y-1.5 text-xs">
                        <div className="flex justify-between">
                          <span className="text-slate-500">Info Rekening:</span>
                          <span className="font-bold text-slate-800 dark:text-slate-200">{bankInfo}</span>
                        </div>
                        <div className="pt-1">
                          <span className="text-slate-500 block mb-1">Keahlian Layanan:</span>
                          <div className="flex flex-wrap gap-1">
                            {(specialties.length > 0 ? specialties : ['Pijat Tradisional', 'Refleksi Kaki']).map((c: string, idx: number) => (
                              <span key={idx} className="px-2 py-0.5 rounded-md bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 text-[10px] font-semibold">
                                {c}
                              </span>
                            ))}
                          </div>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-2 pt-1">
                        <button
                          onClick={async () => {
                            if (confirm(`Tolak pendaftaran mitra ${candidate.name}?`)) {
                              await updateTherapist(candidate.id, {
                                tier_badge: 'Pendaftaran Ditolak'
                              });
                              onRefresh();
                            }
                          }}
                          className="py-2 px-3 rounded-xl bg-rose-50 hover:bg-rose-100 dark:bg-rose-500/10 dark:hover:bg-rose-500/20 text-rose-700 dark:text-rose-400 text-xs font-bold border border-rose-200 dark:border-rose-500/20 transition"
                        >
                          ❌ Tolak Berkas
                        </button>
                        <button
                          onClick={async () => {
                            await updateTherapist(candidate.id, {
                              tier_badge: 'Mitra Terverifikasi',
                              deposit_balance: 100000
                            });

                            // Kirim notifikasi WhatsApp ke mitra via Fonnte
                            try {
                              let clean = (candidate.phone || '').replace(/\D/g, '');
                              if (clean.startsWith('0')) clean = '62' + clean.substring(1);
                              if (!clean.startsWith('62')) clean = '62' + clean;

                              await fetch('https://api.fonnte.com/send', {
                                method: 'POST',
                                headers: {
                                  'Authorization': 'G7i1MwMXPn2pKoSd9HiF',
                                  'Content-Type': 'application/json',
                                },
                                body: JSON.stringify({
                                  target: clean,
                                  message: `*Pendaftaran Mitra Disetujui!* 🎉\n\nHalo *${candidate.name}*,\n\nSelamat! Berkas KYC pendaftaran mitra terapis MassaGo Anda telah *DISETUJUI* oleh Admin. Anda sekarang dapat login ke aplikasi MassaGo Mitra dan mengaktifkan status *ONLINE* untuk mulai menerima pesanan.\n\nSelamat bekerja & sukses selalu bersama MassaGo! 💆‍♂️`,
                                  countryCode: '62',
                                }),
                              });
                            } catch (_) {}

                            alert(`Selamat! Akun mitra ${candidate.name} berhasil disetujui & diaktifkan.`);
                            onRefresh();
                          }}
                          className="py-2 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md transition"
                        >
                          ✅ Setujui Mitra
                        </button>
                      </div>
                    </div>
                  );
                })}
            </div>
          )}
        </div>
      )}

      {/* Therapist Edit & Deposit Adjustment Modal */}
      {selectedTherapist && (
        <Modal
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          title={`Kelola Mitra: ${selectedTherapist.name}`}
          subtitle={`ID Mitra: ${selectedTherapist.id}`}
        >
          <div className="space-y-5">
            {/* Quick Status Switcher */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-2">
                Ubah Status Tugas Mitra:
              </label>
              <div className="grid grid-cols-3 gap-2">
                {(['ONLINE', 'ON_DUTY_BUSY', 'OFFLINE'] as const).map((st) => (
                  <button
                    key={st}
                    onClick={() => handleUpdateStatus(selectedTherapist, st)}
                    disabled={isSubmitting}
                    className={`py-2 px-3 rounded-xl text-xs font-bold transition border ${
                      selectedTherapist.duty_status === st
                        ? 'bg-emerald-600 border-emerald-500 text-white shadow-md'
                        : 'bg-slate-100 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:text-white'
                    }`}
                  >
                    {st === 'ONLINE' ? '🟢 Online' : st === 'ON_DUTY_BUSY' ? '🟠 Sibuk' : '⚪ Offline'}
                  </button>
                ))}
              </div>
            </div>

            {/* Deposit Balance Adjustment Form */}
            <div className="bg-slate-50 dark:bg-slate-950/60 p-4 rounded-xl border border-slate-200 dark:border-slate-800 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">Saldo Deposit Saat Ini:</span>
                <span className="text-sm font-bold text-emerald-600 dark:text-emerald-400 font-mono">
                  Rp {Number(selectedTherapist.deposit_balance || 100000).toLocaleString('id-ID')}
                </span>
              </div>

              <div>
                <label className="block text-xs text-slate-500 dark:text-slate-400 mb-1">Jumlah Penyesuaian Saldo (Rp):</label>
                <input
                  type="number"
                  placeholder="Contoh: 50000"
                  value={depositAmount}
                  onChange={(e) => setDepositAmount(e.target.value)}
                  className="w-full px-3.5 py-2 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500 font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-2 pt-2">
                <button
                  onClick={() => handleDepositAdjustment(false)}
                  disabled={isSubmitting || !depositAmount}
                  className="py-2 px-3 rounded-xl bg-rose-50 hover:bg-rose-100 dark:bg-rose-500/10 dark:hover:bg-rose-500/20 text-rose-700 dark:text-rose-400 text-xs font-bold border border-rose-200 dark:border-rose-500/20 transition disabled:opacity-50"
                >
                  - Potong Deposit
                </button>
                <button
                  onClick={() => handleDepositAdjustment(true)}
                  disabled={isSubmitting || !depositAmount}
                  className="py-2 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md transition disabled:opacity-50"
                >
                  + Tambah Saldo Deposit
                </button>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
