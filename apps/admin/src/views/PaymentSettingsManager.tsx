'use client';

import React, { useState, useEffect } from 'react';
import {
  CreditCard,
  QrCode,
  Building2,
  Plus,
  Trash2,
  Edit2,
  Check,
  Copy,
  Save,
  Percent,
  Phone,
  ShieldCheck,
  CheckCircle,
  XCircle,
  AlertCircle,
  RefreshCw,
  ExternalLink,
  Sparkles,
  ToggleLeft,
  ToggleRight,
  Lock,
  Eye,
  EyeOff,
  HelpCircle,
} from 'lucide-react';
import { PlatformBankAccount } from '../types';
import {
  getBankAccounts,
  saveBankAccount,
  deleteBankAccount,
  getPlatformSettings,
  updatePlatformSetting,
} from '../lib/supabase';

type PaymentTab = 'gateway' | 'banks' | 'commission_qris' | 'support';

export const PaymentSettingsManager: React.FC = () => {
  const [activeTab, setActiveTab] = useState<PaymentTab>('gateway');
  const [bankAccounts, setBankAccounts] = useState<PlatformBankAccount[]>([]);
  const [settings, setSettings] = useState<Record<string, string>>({});

  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [editingBank, setEditingBank] = useState<Partial<PlatformBankAccount> | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Bank Account Form
  const [formBankName, setFormBankName] = useState('');
  const [formAccountNumber, setFormAccountNumber] = useState('');
  const [formAccountHolder, setFormAccountHolder] = useState('');
  const [formIsActive, setFormIsActive] = useState(true);

  // Gateway (Midtrans / DOKU) Form
  const [gatewayProvider, setGatewayProvider] = useState<'midtrans' | 'doku'>('midtrans');
  const [gatewayEnabled, setGatewayEnabled] = useState(true);
  const [gatewayIsProd, setGatewayIsProd] = useState(false);
  const [midtransMerchantId, setMidtransMerchantId] = useState('');
  const [midtransClientKey, setMidtransClientKey] = useState('');
  const [midtransServerKey, setMidtransServerKey] = useState('');
  const [dokuClientId, setDokuClientId] = useState('');
  const [dokuApiKey, setDokuApiKey] = useState('');
  const [dokuSecretKey, setDokuSecretKey] = useState('');
  const [showKeys, setShowKeys] = useState(false);

  // QRIS & Commission
  const [qrisUrl, setQrisUrl] = useState('');
  const [qrisMerchant, setQrisMerchant] = useState('');
  const [qrisNmid, setQrisNmid] = useState('');
  const [commissionPercent, setCommissionPercent] = useState('20');
  const [minDepositBalance, setMinDepositBalance] = useState('50000');

  // Repeat Order Bonus for Favorite Therapists
  const [repeatBonusActive, setRepeatBonusActive] = useState(true);
  const [repeatBonusType, setRepeatBonusType] = useState<'FIXED' | 'PERCENTAGE'>('FIXED');
  const [repeatBonusValue, setRepeatBonusValue] = useState('15000');

  // Support
  const [adminWa, setAdminWa] = useState('');
  const [supportEmail, setSupportEmail] = useState('');

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [banks, st] = await Promise.all([
        getBankAccounts(),
        getPlatformSettings(),
      ]);
      setBankAccounts(banks);
      setSettings(st);

      // Populate settings
      setGatewayProvider((st.payment_gateway_provider as any) || 'midtrans');
      setGatewayEnabled(st.doku_enabled !== 'false' && st.midtrans_enabled !== 'false');
      setGatewayIsProd(st.doku_is_production === 'true' || st.midtrans_is_production === 'true');

      // Midtrans
      setMidtransMerchantId(st.midtrans_merchant_id || 'G123456789');
      setMidtransClientKey(st.midtrans_client_key || 'SB-Mid-client-XXXXX');
      setMidtransServerKey(st.midtrans_server_key || 'SB-Mid-server-YYYYY');

      // DOKU
      setDokuClientId(st.doku_client_id || 'BRN-0242-1787022128265');
      setDokuApiKey(st.doku_api_key || 'doku_key_4206227d89174879acb1973748f15cc8');
      setDokuSecretKey(st.doku_secret_key || '');

      // QRIS & Commission
      setQrisUrl(st.qris_image_url || '/qris-massago.png');
      setQrisMerchant(st.qris_merchant_name || 'PIJATIN INDONESIA');
      setQrisNmid(st.qris_nmid || 'ID1020030040050');
      setCommissionPercent(st.platform_commission_percent || '20');
      setMinDepositBalance(st.min_deposit_balance || '50000');

      // Repeat Order Loyalty Bonus
      setRepeatBonusActive(st.repeat_order_bonus_active !== 'false');
      setRepeatBonusType((st.repeat_order_bonus_type as any) === 'PERCENTAGE' ? 'PERCENTAGE' : 'FIXED');
      setRepeatBonusValue(st.repeat_order_bonus_value || '15000');

      // Support
      setAdminWa(st.admin_whatsapp || '+6281234567890');
      setSupportEmail(st.support_email || 'support@massago.id');
    } catch (err) {
      console.error('Error loading payment data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleOpenAddModal = () => {
    setEditingBank(null);
    setFormBankName('');
    setFormAccountNumber('');
    setFormAccountHolder('');
    setFormIsActive(true);
    setShowModal(true);
  };

  const handleOpenEditModal = (bank: PlatformBankAccount) => {
    setEditingBank(bank);
    setFormBankName(bank.bank_name);
    setFormAccountNumber(bank.account_number);
    setFormAccountHolder(bank.account_holder);
    setFormIsActive(bank.is_active);
    setShowModal(true);
  };

  const handleSaveBank = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formBankName || !formAccountNumber || !formAccountHolder) {
      alert('Mohon isi semua data rekening dengan lengkap.');
      return;
    }

    const payload: Partial<PlatformBankAccount> = {
      bank_name: formBankName.trim(),
      account_number: formAccountNumber.trim(),
      account_holder: formAccountHolder.trim(),
      is_active: formIsActive,
    };

    if (editingBank?.id) {
      payload.id = editingBank.id;
    }

    const success = await saveBankAccount(payload);
    if (success) {
      showToast(editingBank ? 'Rekening berhasil diperbarui!' : 'Rekening baru berhasil ditambahkan!');
      setShowModal(false);
      loadData();
    } else {
      alert('Gagal menyimpan rekening ke database.');
    }
  };

  const handleDeleteBank = async (id: string, name: string) => {
    if (confirm(`Apakah Anda yakin ingin menghapus rekening ${name}?`)) {
      const success = await deleteBankAccount(id);
      if (success) {
        showToast('Rekening berhasil dihapus.');
        loadData();
      } else {
        alert('Gagal menghapus rekening.');
      }
    }
  };

  const handleToggleActive = async (bank: PlatformBankAccount) => {
    const nextState = !bank.is_active;
    const success = await saveBankAccount({ ...bank, is_active: nextState });
    if (success) {
      showToast(`Rekening ${bank.bank_name} ${nextState ? 'diaktifkan' : 'dinonaktifkan'}.`);
      loadData();
    }
  };

  const handleSaveAllSettings = async () => {
    setIsSaving(true);
    try {
      await Promise.all([
        updatePlatformSetting('payment_gateway_provider', gatewayProvider, 'Provider Payment Gateway'),
        updatePlatformSetting('midtrans_enabled', gatewayEnabled ? 'true' : 'false', 'Status Midtrans'),
        updatePlatformSetting('midtrans_is_production', gatewayIsProd ? 'true' : 'false', 'Midtrans Environment'),
        updatePlatformSetting('midtrans_merchant_id', midtransMerchantId.trim(), 'Midtrans Merchant ID'),
        updatePlatformSetting('midtrans_client_key', midtransClientKey.trim(), 'Midtrans Client Key'),
        updatePlatformSetting('midtrans_server_key', midtransServerKey.trim(), 'Midtrans Server Key'),
        updatePlatformSetting('doku_enabled', gatewayEnabled ? 'true' : 'false', 'Status DOKU'),
        updatePlatformSetting('doku_is_production', gatewayIsProd ? 'true' : 'false', 'DOKU Environment'),
        updatePlatformSetting('doku_client_id', dokuClientId.trim(), 'DOKU Client ID'),
        updatePlatformSetting('doku_api_key', dokuApiKey.trim(), 'DOKU API Key'),
        updatePlatformSetting('doku_secret_key', dokuSecretKey.trim(), 'DOKU Secret Key'),
        updatePlatformSetting('qris_image_url', qrisUrl.trim(), 'URL Gambar QRIS'),
        updatePlatformSetting('qris_merchant_name', qrisMerchant.trim(), 'Nama Merchant QRIS'),
        updatePlatformSetting('qris_nmid', qrisNmid.trim(), 'NMID QRIS'),
        updatePlatformSetting('platform_commission_percent', commissionPercent.trim(), 'Persentase Komisi Platform'),
        updatePlatformSetting('min_deposit_balance', minDepositBalance.trim(), 'Batas Minimal Saldo Deposit'),
        updatePlatformSetting('repeat_order_bonus_active', repeatBonusActive ? 'true' : 'false', 'Status Bonus Repeat Order'),
        updatePlatformSetting('repeat_order_bonus_type', repeatBonusType, 'Tipe Bonus Repeat Order'),
        updatePlatformSetting('repeat_order_bonus_value', repeatBonusValue.trim(), 'Nilai Bonus Repeat Order'),
        updatePlatformSetting('admin_whatsapp', adminWa.trim(), 'WhatsApp CS Admin'),
        updatePlatformSetting('support_email', supportEmail.trim(), 'Email Support Platform'),
      ]);
      showToast('Semua pengaturan pembayaran berhasil disimpan & tersinkronisasi!');
      loadData();
    } catch (err) {
      console.error('Error saving settings:', err);
      alert('Gagal menyimpan pengaturan.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-6 right-6 z-50 bg-slate-900 text-white px-4 py-3 rounded-2xl shadow-2xl flex items-center gap-2.5 border border-emerald-500/40 text-xs font-semibold">
          <CheckCircle className="w-4 h-4 text-emerald-400" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Header & Main Save Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">Pengaturan Pembayaran</h1>
            <span className="px-2 py-0.5 rounded-full bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 text-[11px] font-bold border border-emerald-200 dark:border-emerald-500/20">
              Live Cloud Config
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Konfigurasi gateway transaksi online, rekening bank transfer manual, dan skema bagi hasil platform.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={loadData}
            disabled={isLoading}
            className="p-2.5 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-xl text-slate-700 dark:text-slate-300 text-xs font-semibold transition flex items-center gap-2 border border-slate-200 dark:border-slate-700"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline">Refresh</span>
          </button>

          <button
            onClick={handleSaveAllSettings}
            disabled={isSaving}
            className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow-md shadow-emerald-600/20 transition flex items-center gap-2 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>{isSaving ? 'Menyimpan...' : 'Simpan Perubahan'}</span>
          </button>
        </div>
      </div>

      {/* Clean Segmented Tabs */}
      <div className="flex items-center gap-1.5 p-1.5 bg-slate-200/60 dark:bg-slate-800/80 rounded-2xl border border-slate-200 dark:border-slate-700/60 text-xs font-semibold">
        <button
          onClick={() => setActiveTab('gateway')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl transition ${
            activeTab === 'gateway'
              ? 'bg-white dark:bg-slate-900 text-emerald-700 dark:text-emerald-400 shadow-sm font-bold'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
          }`}
        >
          <CreditCard className="w-4 h-4" />
          <span>Payment Gateway (Midtrans)</span>
        </button>

        <button
          onClick={() => setActiveTab('banks')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl transition ${
            activeTab === 'banks'
              ? 'bg-white dark:bg-slate-900 text-emerald-700 dark:text-emerald-400 shadow-sm font-bold'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
          }`}
        >
          <Building2 className="w-4 h-4" />
          <span>Rekening Bank Platform ({bankAccounts.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('commission_qris')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl transition ${
            activeTab === 'commission_qris'
              ? 'bg-white dark:bg-slate-900 text-emerald-700 dark:text-emerald-400 shadow-sm font-bold'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
          }`}
        >
          <Percent className="w-4 h-4" />
          <span>Bagi Hasil & QRIS Statis</span>
        </button>

        <button
          onClick={() => setActiveTab('support')}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl transition ${
            activeTab === 'support'
              ? 'bg-white dark:bg-slate-900 text-emerald-700 dark:text-emerald-400 shadow-sm font-bold'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
          }`}
        >
          <Phone className="w-4 h-4" />
          <span>Kontak Bantuan & CS</span>
        </button>
      </div>

      {/* Tab Content 1: Payment Gateway */}
      {activeTab === 'gateway' && (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-slate-100 dark:border-slate-800">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-emerald-600" />
                Integrasi Gateway Pembayaran Otomatis
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                Menerima pembayaran instan via QRIS Gopay/OVO/ShopeePay, Virtual Account (BCA, Mandiri, BRI, BNI), dan Kartu Kredit.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 dark:text-slate-300 cursor-pointer">
                <input
                  type="checkbox"
                  checked={gatewayEnabled}
                  onChange={(e) => setGatewayEnabled(e.target.checked)}
                  className="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4"
                />
                <span>Aktifkan Gateway</span>
              </label>

              <div className="h-4 w-px bg-slate-200 dark:bg-slate-700" />

              <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                gatewayIsProd
                  ? 'bg-rose-50 dark:bg-rose-500/10 text-rose-700 dark:text-rose-400 border border-rose-200'
                  : 'bg-amber-50 dark:bg-amber-500/10 text-amber-700 dark:text-amber-400 border border-amber-200'
              }`}>
                {gatewayIsProd ? '🔴 Mode Production (Live)' : '🟡 Mode Sandbox (Testing)'}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Environment Toggle */}
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                Environment Server
              </label>
              <select
                value={gatewayIsProd ? 'production' : 'sandbox'}
                onChange={(e) => setGatewayIsProd(e.target.value === 'production')}
                className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              >
                <option value="sandbox">Sandbox (Development / Testing Bebas Biaya)</option>
                <option value="production">Production (Real Transaksi Uang Nyata)</option>
              </select>
              <p className="text-[11px] text-slate-500">
                Pilih Sandbox saat proses uji coba, dan ubah ke Production saat peluncuran resmi.
              </p>
            </div>

            {/* Merchant ID */}
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                Midtrans Merchant ID
              </label>
              <input
                type="text"
                placeholder="G123456789"
                value={midtransMerchantId}
                onChange={(e) => setMidtransMerchantId(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-mono rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
              <p className="text-[11px] text-slate-500">Dapat ditemukan di Dashboard Midtrans &gt; Settings &gt; General.</p>
            </div>

            {/* Client Key */}
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                Midtrans Client Key
              </label>
              <input
                type="text"
                placeholder="SB-Mid-client-..."
                value={midtransClientKey}
                onChange={(e) => setMidtransClientKey(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-mono rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
              <p className="text-[11px] text-slate-500">Kunci publik yang digunakan aplikasi Customer & Mitra.</p>
            </div>

            {/* Server Key */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Midtrans Server Key (Rahasia)
                </label>
                <button
                  type="button"
                  onClick={() => setShowKeys(!showKeys)}
                  className="text-[11px] text-emerald-600 hover:underline flex items-center gap-1 font-medium"
                >
                  {showKeys ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
                  <span>{showKeys ? 'Sembunyikan' : 'Tampilkan'}</span>
                </button>
              </div>
              <input
                type={showKeys ? 'text' : 'password'}
                placeholder="SB-Mid-server-..."
                value={midtransServerKey}
                onChange={(e) => setMidtransServerKey(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-mono rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
              <p className="text-[11px] text-slate-500">Kunci otorisasi backend untuk memverifikasi callback pembayaran.</p>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200 dark:border-slate-800 flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
            <div className="text-xs text-slate-600 dark:text-slate-300 space-y-1">
              <span className="font-bold text-slate-900 dark:text-white block">URL Notification Webhook Callback:</span>
              <div className="flex items-center gap-2 font-mono text-[11px] bg-white dark:bg-slate-900 p-2 rounded-lg border border-slate-200 dark:border-slate-800">
                <span className="text-emerald-600 truncate">https://massago.id/api/midtrans/webhook</span>
                <button
                  onClick={() => handleCopy('https://massago.id/api/midtrans/webhook', 'webhook')}
                  className="ml-auto text-slate-400 hover:text-slate-700 dark:hover:text-white text-xs"
                >
                  {copiedId === 'webhook' ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
              <p className="text-[11px] text-slate-500">
                Pasang URL ini pada menu <i>Configuration &gt; Payment Notification URL</i> di Dashboard Midtrans Anda.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Tab Content 2: Bank Accounts */}
      {activeTab === 'banks' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white">Daftar Rekening Bank Platform</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Rekening tujuan resmi untuk transfer manual top-up deposit mitra terapis dan pembayaran customer.
              </p>
            </div>
            <button
              onClick={handleOpenAddModal}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow-sm transition flex items-center gap-1.5"
            >
              <Plus className="w-4 h-4" />
              <span>Tambah Rekening</span>
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {bankAccounts.map((bank) => (
              <div
                key={bank.id}
                className={`p-5 rounded-2xl border transition relative bg-white dark:bg-slate-900 ${
                  bank.is_active
                    ? 'border-slate-200 dark:border-slate-800 shadow-sm'
                    : 'border-slate-200/60 dark:border-slate-800/40 opacity-60'
                }`}
              >
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 flex items-center justify-center font-black text-emerald-700 dark:text-emerald-400 text-xs">
                      {bank.bank_name.substring(0, 3).toUpperCase()}
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-slate-900 dark:text-white">{bank.bank_name}</h4>
                      <span className={`text-[10px] font-semibold ${bank.is_active ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-400'}`}>
                        {bank.is_active ? '● Aktif Digunakan' : '○ Dinonaktifkan'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => handleOpenEditModal(bank)}
                      className="p-1.5 text-slate-400 hover:text-slate-700 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition"
                      title="Edit Rekening"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => handleDeleteBank(bank.id, bank.bank_name)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-500/10 rounded-lg transition"
                      title="Hapus Rekening"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                <div className="space-y-1 bg-slate-50 dark:bg-slate-950/60 p-3 rounded-xl border border-slate-100 dark:border-slate-800/80">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] text-slate-500">Nomor Rekening:</span>
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono font-bold text-xs text-slate-900 dark:text-white">{bank.account_number}</span>
                      <button
                        onClick={() => handleCopy(bank.account_number, bank.id)}
                        className="text-slate-400 hover:text-slate-700 dark:hover:text-white"
                      >
                        {copiedId === bank.id ? <Check className="w-3 h-3 text-emerald-500" /> : <Copy className="w-3 h-3" />}
                      </button>
                    </div>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] text-slate-500">Atas Nama:</span>
                    <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">{bank.account_holder}</span>
                  </div>
                </div>

                <button
                  onClick={() => handleToggleActive(bank)}
                  className={`w-full mt-3 py-1.5 rounded-xl text-xs font-semibold border transition ${
                    bank.is_active
                      ? 'bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-700'
                      : 'bg-emerald-50 dark:bg-emerald-500/10 hover:bg-emerald-100 text-emerald-700 dark:text-emerald-400 border-emerald-200 dark:border-emerald-500/20'
                  }`}
                >
                  {bank.is_active ? 'Nonaktifkan Rekening' : 'Aktifkan Rekening'}
                </button>
              </div>
            ))}

            {bankAccounts.length === 0 && (
              <div className="col-span-full p-8 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 text-slate-400 text-xs">
                Belum ada rekening bank yang didaftarkan. Klik "Tambah Rekening" di atas.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab Content 3: Commission & Static QRIS */}
      {activeTab === 'commission_qris' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Commission Card */}
          <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <Percent className="w-4 h-4 text-emerald-600" />
              Skema Komisi & Bagi Hasil Platform
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Persentase potongan bagi hasil aplikasi atas setiap transaksi order yang selesai dikerjakan mitra terapis.
            </p>

            <div className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Potongan Komisi Platform (%)
                </label>
                <div className="relative">
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={commissionPercent}
                    onChange={(e) => setCommissionPercent(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-bold rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">%</span>
                </div>
                <p className="text-[11px] text-slate-500">
                  Contoh: Jika 20%, mitra terapis menerima 80% dari tarif layanan secara bersih.
                </p>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Batas Minimal Saldo Deposit Mitra (Rp)
                </label>
                <input
                  type="number"
                  min="0"
                  value={minDepositBalance}
                  onChange={(e) => setMinDepositBalance(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-bold rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                />
                <p className="text-[11px] text-slate-500">
                  Mitra harus memiliki saldo deposit minimal ini untuk dapat mengaktifkan status Online.
                </p>
              </div>
            </div>
          </div>

          {/* Repeat Order Bonus Card */}
          <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-amber-500" />
                Bonus Repeat Order (Pelanggan Langganan)
              </h3>
              <button
                onClick={() => setRepeatBonusActive(!repeatBonusActive)}
                className={`p-1 rounded-full transition ${
                  repeatBonusActive ? 'text-amber-500' : 'text-slate-400'
                }`}
                title={repeatBonusActive ? 'Bonus Aktif' : 'Bonus Non-Aktif'}
              >
                {repeatBonusActive ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
              </button>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Insentif tambahan yang diberikan kepada mitra terapis ketika pelanggan melakukan pemesanan ulang secara langsung (Terapis Favorit).
            </p>

            <div className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Tipe Perhitungan Bonus
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRepeatBonusType('FIXED')}
                    className={`py-2 px-3 rounded-xl text-xs font-bold border transition ${
                      repeatBonusType === 'FIXED'
                        ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/40'
                        : 'bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-700'
                    }`}
                  >
                    Nominal Tetap (Rp)
                  </button>
                  <button
                    type="button"
                    onClick={() => setRepeatBonusType('PERCENTAGE')}
                    className={`py-2 px-3 rounded-xl text-xs font-bold border transition ${
                      repeatBonusType === 'PERCENTAGE'
                        ? 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/40'
                        : 'bg-slate-50 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-700'
                    }`}
                  >
                    Persentase (%)
                  </button>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  {repeatBonusType === 'FIXED' ? 'Nominal Bonus per Order (Rp)' : 'Persentase Bonus dari Tarif (%)'}
                </label>
                <div className="relative">
                  <input
                    type="number"
                    min="0"
                    value={repeatBonusValue}
                    onChange={(e) => setRepeatBonusValue(e.target.value)}
                    className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-bold rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-amber-500"
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                    {repeatBonusType === 'FIXED' ? 'IDR' : '%'}
                  </span>
                </div>
                <p className="text-[11px] text-slate-500">
                  {repeatBonusType === 'FIXED'
                    ? `Setiap order langganan selesai, mitra mendapat tambahan bonus Rp ${Number(repeatBonusValue || 0).toLocaleString('id-ID')}.`
                    : `Setiap order langganan selesai, mitra mendapat tambahan bonus ${repeatBonusValue}% dari harga paket.`}
                </p>
              </div>
            </div>
          </div>

          {/* QRIS Card */}
          <div className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <QrCode className="w-4 h-4 text-emerald-600" />
              QRIS Statis Resmi Platform
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              QRIS statis merchant yang ditampilkan pada aplikasi saat mitra melakukan top up saldo manual.
            </p>

            <div className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  Nama Merchant QRIS
                </label>
                <input
                  type="text"
                  placeholder="PIJATIN INDONESIA"
                  value={qrisMerchant}
                  onChange={(e) => setQrisMerchant(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-semibold rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  NMID QRIS
                </label>
                <input
                  type="text"
                  placeholder="ID1020030040050"
                  value={qrisNmid}
                  onChange={(e) => setQrisNmid(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-mono rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                  URL / Path Gambar QRIS
                </label>
                <input
                  type="text"
                  placeholder="/qris-massago.png"
                  value={qrisUrl}
                  onChange={(e) => setQrisUrl(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab Content 4: Support & WhatsApp */}
      {activeTab === 'support' && (
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 shadow-sm space-y-6">
          <div>
            <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <Phone className="w-4 h-4 text-emerald-600" />
              Kontak Bantuan & Customer Service Resmi
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Kontak ini akan terhubung langsung saat pengguna atau mitra menekan tombol "Hubungi CS / Bantuan" di aplikasi.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                Nomor WhatsApp Admin CS
              </label>
              <input
                type="text"
                placeholder="+6281234567890"
                value={adminWa}
                onChange={(e) => setAdminWa(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-mono rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
              <p className="text-[11px] text-slate-500">Gunakan format internasional dengan kode negara (contoh: +6281234567890).</p>
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">
                Email Dukungan Platform
              </label>
              <input
                type="email"
                placeholder="support@massago.id"
                value={supportEmail}
                onChange={(e) => setSupportEmail(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
              />
              <p className="text-[11px] text-slate-500">Email resmi korespondensi untuk kebutuhan verifikasi & laporan kendala.</p>
            </div>
          </div>
        </div>
      )}

      {/* Modal Add / Edit Bank Account */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-6 w-full max-w-md shadow-2xl space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
              <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                {editingBank ? 'Edit Rekening Bank' : 'Tambah Rekening Bank Baru'}
              </h3>
              <button
                onClick={() => setShowModal(false)}
                className="text-slate-400 hover:text-slate-700 dark:hover:text-white text-sm"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveBank} className="space-y-4">
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">Nama Bank</label>
                <input
                  type="text"
                  placeholder="Contoh: BCA, Mandiri, BRI, BNI"
                  value={formBankName}
                  onChange={(e) => setFormBankName(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">Nomor Rekening</label>
                <input
                  type="text"
                  placeholder="Contoh: 8406731340"
                  value={formAccountNumber}
                  onChange={(e) => setFormAccountNumber(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white font-mono rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-slate-700 dark:text-slate-300">Nama Pemilik Rekening (Atas Nama)</label>
                <input
                  type="text"
                  placeholder="Contoh: PT MassaGo Sejahtera Indonesia"
                  value={formAccountHolder}
                  onChange={(e) => setFormAccountHolder(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white rounded-xl border border-slate-200 dark:border-slate-700 focus:outline-none focus:border-emerald-500"
                  required
                />
              </div>

              <label className="flex items-center gap-2 pt-1 text-xs font-semibold text-slate-700 dark:text-slate-300 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formIsActive}
                  onChange={(e) => setFormIsActive(e.target.checked)}
                  className="rounded text-emerald-600 focus:ring-emerald-500 w-4 h-4"
                />
                <span>Jadikan Rekening Aktif</span>
              </label>

              <div className="grid grid-cols-2 gap-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="py-2.5 px-4 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 text-slate-700 dark:text-slate-300 text-xs font-bold transition"
                >
                  Batal
                </button>
                <button
                  type="submit"
                  className="py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md transition"
                >
                  Simpan Rekening
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
