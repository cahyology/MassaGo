'use client';

import React, { useState } from 'react';
import {
  AlertTriangle,
  ShieldAlert,
  PhoneCall,
  MapPin,
  Clock,
  CheckCircle,
  AlertCircle,
  Radio,
  FileCheck,
  ShieldCheck,
} from 'lucide-react';
import { Badge } from '../components/common/Badge';
import { SosAlert } from '../types';

export const SosIncidentCenter: React.FC = () => {
  // Real live alerts list (starts empty when no emergency is active)
  const [alerts, setAlerts] = useState<SosAlert[]>([]);

  const handleResolveAlert = (id: string) => {
    setAlerts((prev) =>
      prev.map((a) => (a.id === id ? { ...a, status: 'RESOLVED' as const } : a))
    );
  };

  const activeAlerts = alerts.filter((a) => a.status === 'ACTIVE' || a.status === 'INVESTIGATING');

  return (
    <div className="space-y-6">
      {/* Top Banner: Emergency Status Header */}
      <div
        className={`p-5 rounded-2xl border transition shadow-sm ${
          activeAlerts.length > 0
            ? 'bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-500/40'
            : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800'
        }`}
      >
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div
              className={`p-3 rounded-2xl ${
                activeAlerts.length > 0
                  ? 'bg-rose-600 text-white animate-bounce'
                  : 'bg-emerald-50 dark:bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/20'
              }`}
            >
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white tracking-tight">
                Pusat Tanggap Darurat & Keselamatan Operasional (24/7)
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                Monitoring terpusat untuk seluruh sinyal SOS yang dipicu oleh mitra terapis maupun pelanggan di aplikasi mobile.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span
              className={`text-xs font-bold px-3 py-1.5 rounded-xl flex items-center gap-1.5 ${
                activeAlerts.length > 0
                  ? 'bg-rose-500 text-white animate-pulse'
                  : 'bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-500/20'
              }`}
            >
              <Radio className="w-3.5 h-3.5" />
              <span>{activeAlerts.length} Insiden Aktif</span>
            </span>
          </div>
        </div>
      </div>

      {/* Incidents List */}
      <div className="space-y-4">
        {alerts.length === 0 ? (
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-12 text-center shadow-sm">
            <ShieldCheck className="w-12 h-12 text-emerald-500 mx-auto mb-3" />
            <h3 className="text-base font-bold text-slate-900 dark:text-white">Situasi Operasional Aman & Terkendali</h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-md mx-auto">
              Tidak ada panggilan darurat (SOS) aktif saat ini. Jika ada mitra atau customer yang memicu tombol bahaya di HP mereka, alarm dan koordinat GPS langsung muncul di sini secara seketika.
            </p>
          </div>
        ) : (
          alerts.map((alert) => {
            const isResolved = alert.status === 'RESOLVED';

            return (
              <div
                key={alert.id}
                className={`bg-white dark:bg-slate-900 border rounded-2xl p-5 shadow-sm space-y-4 transition ${
                  isResolved
                    ? 'border-slate-200 dark:border-slate-800/80 opacity-75'
                    : 'border-rose-300 dark:border-rose-500/30 bg-rose-50/40 dark:bg-rose-950/10'
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-100 dark:border-slate-800">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm ${
                        alert.sender_type === 'THERAPIST'
                          ? 'bg-amber-50 dark:bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-500/20'
                          : 'bg-sky-50 dark:bg-sky-500/10 text-sky-600 dark:text-sky-400 border border-sky-200 dark:border-sky-500/20'
                      }`}
                    >
                      {alert.sender_type === 'THERAPIST' ? '🛵' : '👤'}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-bold text-slate-900 dark:text-white">{alert.sender_name}</h3>
                        <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                          {alert.sender_type === 'THERAPIST' ? 'Mitra Terapis' : 'Pelanggan'}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 dark:text-slate-400">
                        ID: {alert.sender_id} • Order ID: {alert.order_id || '-'}
                      </p>
                    </div>
                  </div>

                  <Badge
                    variant={isResolved ? 'emerald' : alert.status === 'ACTIVE' ? 'rose' : 'amber'}
                    size="sm"
                    pulse={!isResolved}
                  >
                    {isResolved ? '✓ Tuntas & Aman' : alert.status === 'ACTIVE' ? '🔴 Sinyal Bahaya' : '🟡 Sedang Ditangani'}
                  </Badge>
                </div>

                {/* Coordinates & Telemetry Box */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs bg-slate-50 dark:bg-slate-950/40 p-3 rounded-xl border border-slate-200 dark:border-slate-800">
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block text-[10px]">Nomor Kontak:</span>
                    <span className="font-mono font-bold text-emerald-600 dark:text-emerald-400">{alert.sender_phone}</span>
                  </div>
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block text-[10px]">Titik GPS Koordinat:</span>
                    <span className="font-mono text-slate-700 dark:text-slate-300">
                      {alert.latitude.toFixed(4)}, {alert.longitude.toFixed(4)}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block text-[10px]">Waktu Sinyal:</span>
                    <span className="text-slate-700 dark:text-slate-300">
                      {new Date(alert.timestamp).toLocaleTimeString('id-ID')} WIB
                    </span>
                  </div>
                </div>

                {/* Notes */}
                {alert.notes && (
                  <div className="text-xs text-slate-700 dark:text-slate-300 bg-slate-100 dark:bg-slate-800/40 p-3 rounded-xl border border-slate-200 dark:border-slate-700/60">
                    <span className="font-semibold text-slate-500 dark:text-slate-400 block text-[10px] uppercase mb-0.5">
                      Catatan Penanganan:
                    </span>
                    {alert.notes}
                  </div>
                )}

                {/* Action Buttons */}
                {!isResolved && (
                  <div className="flex items-center justify-end gap-2 pt-2">
                    <a
                      href={`tel:${alert.sender_phone}`}
                      className="py-2 px-3 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 text-xs font-semibold border border-slate-200 dark:border-slate-700 transition flex items-center gap-1.5"
                    >
                      <PhoneCall className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                      <span>Hubungi Nomor</span>
                    </a>

                    <button
                      onClick={() => handleResolveAlert(alert.id)}
                      className="py-2 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md shadow-emerald-600/30 transition flex items-center gap-1.5"
                    >
                      <ShieldCheck className="w-3.5 h-3.5" />
                      <span>Tandai Selesai & Aman</span>
                    </button>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
