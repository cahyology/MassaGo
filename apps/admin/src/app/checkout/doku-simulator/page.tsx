'use client';

import React, { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { CheckCircle2, QrCode, Building2, CreditCard, ShieldCheck, ArrowRight, RefreshCw, Smartphone } from 'lucide-react';

function SimulatorContent() {
  const searchParams = useSearchParams();
  const invoice = searchParams.get('invoice') || `INV-${Date.now()}`;
  const amountStr = searchParams.get('amount') || '160000';
  const service = searchParams.get('service') || 'Pijat Tradisional Jawa (90 Menit)';
  const type = searchParams.get('type') || 'ORDER';
  const orderId = searchParams.get('order_id') || '';
  const therapistId = searchParams.get('therapist_id') || '';

  const amount = Number(amountStr) || 160000;
  const [selectedMethod, setSelectedMethod] = useState<'QRIS' | 'BCA_VA' | 'MANDIRI_VA' | 'GOPAY'>('QRIS');
  const [isProcessing, setIsProcessing] = useState(false);
  const [isPaid, setIsPaid] = useState(false);

  const formatRupiah = (val: number) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  const handleSimulatePayment = async () => {
    setIsProcessing(true);
    try {
      const res = await fetch('/api/doku/notify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          order: {
            invoice_number: invoice,
            amount: amount,
          },
          transaction: {
            status: 'SUCCESS',
            date: new Date().toISOString(),
          },
          additional_info: {
            type,
            order_id: orderId,
            therapist_id: therapistId,
            service_name: service,
          },
        }),
      });

      if (res.ok) {
        setIsPaid(true);
      } else {
        alert('Gagal memproses notifikasi simulasi.');
      }
    } catch (err) {
      console.error(err);
      alert('Koneksi gagal saat menghubungi webhook.');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-slate-850 bg-slate-800/90 border border-slate-700/80 rounded-3xl p-6 shadow-2xl space-y-6 backdrop-blur-xl">
        {/* DOKU Header Badge */}
        <div className="flex items-center justify-between border-b border-slate-700/60 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-rose-600 flex items-center justify-center font-black text-white text-xs tracking-tighter">
              DOKU
            </div>
            <div>
              <div className="text-xs font-bold text-white tracking-wide">DOKU PAYMENT GATEWAY</div>
              <div className="text-[10px] text-emerald-400 font-semibold flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                Secure Sandbox Simulator
              </div>
            </div>
          </div>
          <ShieldCheck className="w-5 h-5 text-emerald-400" />
        </div>

        {isPaid ? (
          /* Payment Success State */
          <div className="py-8 text-center space-y-4">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center mx-auto animate-bounce">
              <CheckCircle2 className="w-9 h-9" />
            </div>
            <div>
              <h3 className="text-lg font-extrabold text-white">Pembayaran Sukses!</h3>
              <p className="text-xs text-slate-400 mt-1">
                {type === 'DEPOSIT_TOPUP'
                  ? 'Saldo deposit mitra telah otomatis ditambahkan ke akun.'
                  : 'Pesanan telah otomatis dibayar dan radar pencarian terapis aktif.'}
              </p>
            </div>

            <div className="bg-slate-900/60 p-4 rounded-2xl border border-slate-700/50 text-left space-y-2 text-xs">
              <div className="flex justify-between text-slate-400">
                <span>Invoice:</span>
                <span className="font-mono text-white font-semibold">{invoice}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Total Bayar:</span>
                <span className="font-bold text-emerald-400">{formatRupiah(amount)}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Waktu Transaksi:</span>
                <span className="text-white">{new Date().toLocaleTimeString('id-ID')} WIB</span>
              </div>
            </div>

            <p className="text-[11px] text-slate-400">
              Anda dapat kembali ke aplikasi, status sudah tersinkronisasi secara otomatis.
            </p>
          </div>
        ) : (
          /* Payment Selection & Action State */
          <>
            {/* Bill Summary */}
            <div className="bg-slate-900/80 border border-slate-700/60 rounded-2xl p-4 space-y-1.5">
              <div className="text-[11px] text-slate-400 font-medium">Tagihan Pembayaran:</div>
              <div className="text-sm font-bold text-white truncate">{service}</div>
              <div className="text-2xl font-black text-emerald-400 pt-1 tracking-tight">
                {formatRupiah(amount)}
              </div>
              <div className="text-[10px] text-slate-500 font-mono pt-1">Invoice: {invoice}</div>
            </div>

            {/* Payment Method Selector */}
            <div className="space-y-2.5">
              <div className="text-xs font-bold text-slate-300">Pilih Saluran Pembayaran:</div>

              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedMethod('QRIS')}
                  className={`p-3 rounded-xl border flex items-center gap-2.5 text-left transition ${
                    selectedMethod === 'QRIS'
                      ? 'border-emerald-500 bg-emerald-500/10 text-white shadow-md shadow-emerald-500/10'
                      : 'border-slate-700 bg-slate-900/40 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  <QrCode className="w-5 h-5 text-emerald-400" />
                  <div>
                    <div className="text-xs font-bold">QRIS Instant</div>
                    <div className="text-[9px] text-slate-400">GoPay, OVO, Dana, BCA</div>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setSelectedMethod('BCA_VA')}
                  className={`p-3 rounded-xl border flex items-center gap-2.5 text-left transition ${
                    selectedMethod === 'BCA_VA'
                      ? 'border-emerald-500 bg-emerald-500/10 text-white shadow-md shadow-emerald-500/10'
                      : 'border-slate-700 bg-slate-900/40 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  <Building2 className="w-5 h-5 text-sky-400" />
                  <div>
                    <div className="text-xs font-bold">BCA VA</div>
                    <div className="text-[9px] text-slate-400">Virtual Account</div>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setSelectedMethod('MANDIRI_VA')}
                  className={`p-3 rounded-xl border flex items-center gap-2.5 text-left transition ${
                    selectedMethod === 'MANDIRI_VA'
                      ? 'border-emerald-500 bg-emerald-500/10 text-white shadow-md shadow-emerald-500/10'
                      : 'border-slate-700 bg-slate-900/40 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  <CreditCard className="w-5 h-5 text-amber-400" />
                  <div>
                    <div className="text-xs font-bold">Mandiri VA</div>
                    <div className="text-[9px] text-slate-400">Virtual Account</div>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setSelectedMethod('GOPAY')}
                  className={`p-3 rounded-xl border flex items-center gap-2.5 text-left transition ${
                    selectedMethod === 'GOPAY'
                      ? 'border-emerald-500 bg-emerald-500/10 text-white shadow-md shadow-emerald-500/10'
                      : 'border-slate-700 bg-slate-900/40 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  <Smartphone className="w-5 h-5 text-emerald-400" />
                  <div>
                    <div className="text-xs font-bold">E-Wallet Direct</div>
                    <div className="text-[9px] text-slate-400">ShopeePay / OVO</div>
                  </div>
                </button>
              </div>
            </div>

            {/* Simulated Action Button */}
            <button
              onClick={handleSimulatePayment}
              disabled={isProcessing}
              className="w-full py-3.5 px-4 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-black rounded-2xl shadow-lg shadow-emerald-500/25 flex items-center justify-center gap-2 text-sm transition transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50"
            >
              {isProcessing ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Memproses Pembayaran DOKU...</span>
                </>
              ) : (
                <>
                  <span>Bayar Sekarang ({formatRupiah(amount)})</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>

            <div className="text-[10px] text-center text-slate-500">
              Transaksi dilindungi enkripsi 256-bit berlisensi Bank Indonesia & DOKU Payment Gateway
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default function DokuSimulatorPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-slate-900 flex items-center justify-center text-white text-xs">Memuat Simulator DOKU...</div>}>
      <SimulatorContent />
    </Suspense>
  );
}
