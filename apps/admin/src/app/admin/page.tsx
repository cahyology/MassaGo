'use client';

import React, { useState, useEffect } from 'react';
import dynamic from 'next/dynamic';
import { Sidebar } from '../../components/layout/Sidebar';
import { Header } from '../../components/layout/Header';
import { DashboardOverview } from '../../views/DashboardOverview';
import { OrdersCenter } from '../../views/OrdersCenter';
import { CustomerManagement } from '../../views/CustomerManagement';
import { MitraManagement } from '../../views/MitraManagement';
import { CatalogPricingManager } from '../../views/CatalogPricingManager';
import { VoucherManager } from '../../views/VoucherManager';
import { SosIncidentCenter } from '../../views/SosIncidentCenter';
import { FinanceLedger } from '../../views/FinanceLedger';
import { PaymentSettingsManager } from '../../views/PaymentSettingsManager';
import { ActiveTab, Therapist, Order, ServicePackage, PromoVoucher, CustomerProfile } from '../../types';
import {
  getTherapists,
  getOrders,
  getCustomers,
  getServicePackages,
  getPromoVouchers,
  supabase,
} from '../../lib/supabase';

// Dynamically import LiveFleetMap with SSR disabled to prevent Leaflet window errors
const LiveFleetMap = dynamic(
  () => import('../../components/map/LiveFleetMap').then((mod) => mod.LiveFleetMap),
  {
    ssr: false,
    loading: () => (
      <div className="h-[calc(100vh-8rem)] bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl flex items-center justify-center text-slate-400 text-xs shadow-sm">
        <span>Memuat Peta Armada Realtime...</span>
      </div>
    ),
  }
);

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('dashboard');
  const [isDarkMode, setIsDarkMode] = useState<boolean>(false); // Default: Light Mode
  const [therapists, setTherapists] = useState<Therapist[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [customers, setCustomers] = useState<CustomerProfile[]>([]);
  const [services, setServices] = useState<ServicePackage[]>([]);
  const [vouchers, setVouchers] = useState<PromoVoucher[]>([]);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Initialize theme from localStorage or default to Light mode
  useEffect(() => {
    const savedTheme = localStorage.getItem('massago_admin_theme');
    if (savedTheme === 'dark') {
      setIsDarkMode(true);
      document.documentElement.classList.add('dark');
      document.documentElement.classList.remove('light');
    } else {
      setIsDarkMode(false);
      document.documentElement.classList.remove('dark');
      document.documentElement.classList.add('light');
    }
  }, []);

  const toggleDarkMode = () => {
    setIsDarkMode((prev) => {
      const next = !prev;
      if (next) {
        document.documentElement.classList.add('dark');
        document.documentElement.classList.remove('light');
        localStorage.setItem('massago_admin_theme', 'dark');
      } else {
        document.documentElement.classList.remove('dark');
        document.documentElement.classList.add('light');
        localStorage.setItem('massago_admin_theme', 'light');
      }
      return next;
    });
  };

  const loadData = async () => {
    setIsRefreshing(true);
    try {
      const [tData, oData, cData, sData, vData] = await Promise.all([
        getTherapists(),
        getOrders(),
        getCustomers(),
        getServicePackages(),
        getPromoVouchers(),
      ]);
      setTherapists(tData);
      setOrders(oData);
      setCustomers(cData);
      setServices(sData);
      setVouchers(vData);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
    } finally {
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();

    // Setup Supabase Realtime Subscription for live order & therapist state
    const channel = supabase
      .channel('admin-dashboard-changes')
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'orders' },
        () => {
          getOrders().then(setOrders);
        }
      )
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'therapists' },
        () => {
          getTherapists().then(setTherapists);
        }
      )
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'sos_emergency_logs' },
        () => {
          // If SOS occurs, force reload
          loadData();
        }
      )
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, []);

  const handleUpdateTherapist = (updated: Therapist) => {
    setTherapists((prev) =>
      prev.map((t) => (t.id === updated.id ? updated : t))
    );
  };

  const handleUpdateOrder = (updated: Order) => {
    setOrders((prev) =>
      prev.map((o) => (o.id === updated.id ? updated : o))
    );
  };

  const handleUpdateServices = (newServices: ServicePackage[]) => {
    setServices(newServices);
  };

  const handleUpdateVouchers = (newVouchers: PromoVoucher[]) => {
    setVouchers(newVouchers);
  };

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50 dark:bg-slate-950 font-sans transition-colors duration-200">
      {/* Dynamic Modern Sidebar */}
      <Sidebar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onlineMitraCount={therapists.filter((t) => t.is_online).length}
        activeOrdersCount={orders.filter((o) => !['COMPLETED_PAYMENT', 'REVIEW_SUBMITTED', 'CANCELLED'].includes(o.status)).length}
        pendingKycCount={therapists.filter((t) => !t.is_active).length}
        activeSosCount={0}
        customersCount={customers.length}
      />

      {/* Main Layout Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Modern Header Navigation */}
        <Header
          activeTab={activeTab}
          isDarkMode={isDarkMode}
          onToggleTheme={toggleDarkMode}
          onRefresh={loadData}
          isRefreshing={isRefreshing}
          activeSosCount={0}
          onSosClick={() => setActiveTab('sos')}
        />

        {/* Scrollable Main Content Container */}
        <main className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8 space-y-6">
          {activeTab === 'dashboard' && (
            <DashboardOverview
              therapists={therapists}
              orders={orders}
              services={services}
              setActiveTab={setActiveTab}
            />
          )}

          {activeTab === 'godview' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-xl font-bold text-slate-900 dark:text-white">
                    Peta Armada Realtime & Radar Dispatch
                  </h1>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Pantau sebaran mitra terapis online, offline, dan rute navigasi pesanan aktif di peta.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400 border border-emerald-200/50">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse mr-1.5"></span>
                    Live GPS Telemetry Active
                  </span>
                </div>
              </div>

              <div className="h-[calc(100vh-13rem)] w-full rounded-2xl overflow-hidden border border-slate-200/80 dark:border-slate-800/80 shadow-sm bg-white dark:bg-slate-900">
                <LiveFleetMap therapists={therapists} orders={orders} />
              </div>
            </div>
          )}

          {activeTab === 'orders' && (
            <OrdersCenter
              orders={orders}
              therapists={therapists}
              onRefresh={loadData}
            />
          )}

          {activeTab === 'mitra' && (
            <MitraManagement
              therapists={therapists}
              onRefresh={loadData}
            />
          )}

          {activeTab === 'customers' && (
            <CustomerManagement
              customers={customers}
              orders={orders}
              therapists={therapists}
              onRefresh={loadData}
            />
          )}

          {activeTab === 'catalog' && (
            <CatalogPricingManager
              services={services}
              onRefresh={loadData}
            />
          )}

          {activeTab === 'vouchers' && (
            <VoucherManager
              vouchers={vouchers}
              onRefresh={loadData}
            />
          )}

          {activeTab === 'finance' && (
            <FinanceLedger
              orders={orders}
              therapists={therapists}
            />
          )}

          {activeTab === 'payments' && (
            <PaymentSettingsManager />
          )}

          {activeTab === 'sos' && (
            <SosIncidentCenter />
          )}
        </main>
      </div>
    </div>
  );
}
