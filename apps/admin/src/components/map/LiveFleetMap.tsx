'use client';

import React, { useState, useEffect } from 'react';
import dynamic from 'next/dynamic';
import {
  MapPin,
  Users,
  Navigation,
  Battery,
  Phone,
  Star,
  Activity,
  Search,
  Compass,
  Radio,
  Clock,
  Layers,
} from 'lucide-react';
import { Badge } from '../common/Badge';
import { Therapist, Order } from '../../types';

interface LiveFleetMapProps {
  therapists: Therapist[];
  orders: Order[];
  isDarkMode?: boolean;
}

// Utility to parse real customer GPS coordinates from order
export function extractCustomerCoords(order: Order): [number, number] {
  const anyOrder = order as any;
  if (typeof anyOrder.customer_lat === 'number' && typeof anyOrder.customer_lng === 'number' && anyOrder.customer_lat !== 0) {
    return [anyOrder.customer_lat, anyOrder.customer_lng];
  }
  if (typeof anyOrder.latitude === 'number' && typeof anyOrder.longitude === 'number' && anyOrder.latitude !== 0) {
    return [anyOrder.latitude, anyOrder.longitude];
  }

  // Parse [GPS:lat,lng] tag from order.address
  if (order.address) {
    const match = order.address.match(/\[GPS:\s*(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)\s*\]/i);
    if (match && match[1] && match[2]) {
      const lat = parseFloat(match[1]);
      const lng = parseFloat(match[2]);
      if (!isNaN(lat) && !isNaN(lng) && lat !== 0 && lng !== 0) {
        return [lat, lng];
      }
    }
  }

  // Fallback to Yogyakarta city center if no GPS found
  return [-7.7956, 110.3695];
}

// Utility to clean raw metadata tags like [GPS:...] and [NOTE:...] from address text
export function cleanAddressText(rawAddress?: string): { address: string; landmark?: string } {
  if (!rawAddress) return { address: 'Yogyakarta' };

  let landmark: string | undefined = undefined;
  const noteMatch = rawAddress.match(/\[NOTE:\s*(.*?)\s*\]/i);
  if (noteMatch && noteMatch[1]) {
    landmark = noteMatch[1];
  }

  const clean = rawAddress
    .replace(/\[GPS:[^\]]*\]/gi, '')
    .replace(/\[NOTE:[^\]]*\]/gi, '')
    .trim();

  return { address: clean || 'Yogyakarta', landmark };
}

// Utility to dynamically compute active duty status based on active ongoing orders
export function computeTherapistEffectiveStatus(
  therapist?: Therapist | null,
  ordersList?: Order[]
): 'ONLINE' | 'ON_DUTY_BUSY' | 'OFFLINE' {
  if (!therapist) return 'OFFLINE';
  const orders = ordersList || [];

  const hasActiveOrder = orders.some((o) => {
    if (!o.therapist_id) return false;
    const cleanOrderId = o.therapist_id.trim();
    const cleanPhone = (therapist.phone || '').replace(/\D/g, '');
    const cleanOrderTherapist = cleanOrderId.replace(/\D/g, '');

    const isMatched =
      cleanOrderId === therapist.id ||
      (cleanPhone && cleanOrderTherapist && cleanPhone === cleanOrderTherapist) ||
      cleanOrderId.includes(therapist.id);

    const isActiveStatus = [
      'ACCEPTED',
      'ACCEPTED_ON_THE_WAY',
      'ARRIVED',
      'ARRIVED_AT_LOCATION',
      'IN_SERVICE',
      'TREATMENT_IN_PROGRESS',
      'SANITATION_AND_PREP',
    ].includes(o.status);

    return isMatched && isActiveStatus;
  });

  if (hasActiveOrder) {
    return 'ON_DUTY_BUSY';
  }
  if (therapist.is_online || therapist.duty_status === 'ONLINE' || therapist.duty_status === 'ON_DUTY_BUSY') {
    return 'ONLINE';
  }
  return 'OFFLINE';
}

// Dynamic Leaflet Map Component with inner Leaflet imports
const MapInner = dynamic(
  async () => {
    const L = (await import('leaflet')).default;
    const { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } = await import('react-leaflet');

    // Create Custom Leaflet DivIcon for motorcycle pins
    const createMotorcycleIcon = (status: string) => {
      const isOnline = status === 'ONLINE';
      const isBusy = status === 'ON_DUTY_BUSY';
      const isOffline = !isOnline && !isBusy;

      const bgColor = isBusy ? '#F59E0B' : isOnline ? '#10B981' : '#64748B';
      const opacity = isOffline ? 'opacity: 0.75;' : '';
      const glow = isOnline
        ? 'box-shadow: 0 0 14px rgba(16, 185, 129, 0.7);'
        : isBusy
        ? 'box-shadow: 0 0 12px rgba(245, 158, 11, 0.6);'
        : '';

      const html = `
        <div style="
          background-color: ${bgColor};
          width: 36px;
          height: 36px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2.5px solid #FFFFFF;
          ${glow}
          ${opacity}
          color: white;
          font-size: 16px;
          cursor: pointer;
          transition: transform 0.2s ease;
        ">
          🛵
        </div>
      `;

      return L.divIcon({
        html: html,
        className: 'custom-fleet-bike',
        iconSize: [36, 36],
        iconAnchor: [18, 18],
        popupAnchor: [0, -18],
      });
    };

    const customerPinIcon = L.divIcon({
      html: `
        <div style="
          background-color: #EF4444;
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2.5px solid #FFFFFF;
          box-shadow: 0 0 12px rgba(239, 68, 68, 0.6);
          color: white;
          font-size: 14px;
          cursor: pointer;
        ">
          📍
        </div>
      `,
      className: 'custom-customer-pin',
      iconSize: [32, 32],
      iconAnchor: [16, 16],
      popupAnchor: [0, -16],
    });

    // Subcomponent to fetch real road routing geometry from OSRM
    function ActiveOrderRouteComponent({
      order,
      matchedTherapist,
    }: {
      order: Order;
      matchedTherapist: Therapist;
    }) {
      const therapistPos: [number, number] = [
        matchedTherapist.latitude || -7.7956,
        matchedTherapist.longitude || 110.3695,
      ];
      const customerPos: [number, number] = extractCustomerCoords(order);
      const { address: cleanAddress, landmark } = cleanAddressText(order.address);

      const [routeCoords, setRouteCoords] = useState<[number, number][]>([
        therapistPos,
        customerPos,
      ]);

      useEffect(() => {
        let isMounted = true;
        async function fetchRoadRoute() {
          try {
            const url = `https://router.project-osrm.org/route/v1/driving/${therapistPos[1]},${therapistPos[0]};${customerPos[1]},${customerPos[0]}?overview=full&geometries=geojson`;
            const res = await fetch(url);
            const data = await res.json();
            if (isMounted && data.routes && data.routes[0]) {
              const coords = data.routes[0].geometry.coordinates.map(
                ([lng, lat]: [number, number]) => [lat, lng] as [number, number]
              );
              setRouteCoords(coords);
            }
          } catch (e) {
            console.error('Failed to fetch road route:', e);
          }
        }
        fetchRoadRoute();
        return () => {
          isMounted = false;
        };
      }, [therapistPos[0], therapistPos[1], customerPos[0], customerPos[1]]);

      return (
        <React.Fragment>
          <Marker position={customerPos} icon={customerPinIcon}>
            <Popup>
              <div className="p-1 text-xs space-y-1">
                <div className="font-bold text-sm text-slate-900 dark:text-white">
                  📍 Penjemputan: {order.customer_name || 'Pelanggan'}
                </div>
                <div className="font-semibold text-emerald-600 dark:text-emerald-400">
                  {order.service_name} • {order.duration_minutes} Mnt
                </div>
                <div className="text-slate-600 dark:text-slate-300 text-[11px] leading-relaxed">
                  {cleanAddress}
                </div>
                {landmark && (
                  <div className="text-amber-700 dark:text-amber-400 text-[10px] font-medium bg-amber-50 dark:bg-amber-500/10 px-2 py-0.5 rounded border border-amber-200 dark:border-amber-500/20">
                    Patokan: {landmark}
                  </div>
                )}
                <div className="text-[10px] font-mono text-slate-400 pt-1 border-t border-slate-200 dark:border-slate-700">
                  GPS: {customerPos[0].toFixed(6)}, {customerPos[1].toFixed(6)}
                </div>
              </div>
            </Popup>
          </Marker>

          {/* Real Street Routing Polyline following actual roads */}
          <Polyline
            positions={routeCoords}
            pathOptions={{
              color: '#10B981',
              weight: 5,
              opacity: 0.9,
            }}
          />
        </React.Fragment>
      );
    }

    return function MapComponent({
      therapists,
      orders,
      isDarkMode,
      selectedTherapist,
      setSelectedTherapist,
      defaultCenter,
    }: {
      therapists: Therapist[];
      orders: Order[];
      isDarkMode: boolean;
      selectedTherapist: Therapist | null;
      setSelectedTherapist: (t: Therapist | null) => void;
      defaultCenter: [number, number];
    }) {
      const lightTile = 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png';
      const darkTile = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';

      const activeOrdersWithRoutes = orders.filter(
        (o) =>
          (o.status === 'ACCEPTED_ON_THE_WAY' || o.status === 'ARRIVED_AT_LOCATION') &&
          o.therapist_id
      );

      return (
        <MapContainer
          center={defaultCenter}
          zoom={13}
          scrollWheelZoom={true}
          style={{ width: '100%', height: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://carto.com/">CARTO</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url={isDarkMode ? darkTile : lightTile}
          />

          {/* Therapists Fleet (Online, Busy, and Offline Last Known GPS) */}
          {therapists.map((therapist) => {
            const pos: [number, number] = [
              therapist.latitude || -7.7956,
              therapist.longitude || 110.3695,
            ];
            const st = computeTherapistEffectiveStatus(therapist, orders);

            return (
              <Marker
                key={therapist.id}
                position={pos}
                icon={createMotorcycleIcon(st)}
                eventHandlers={{
                  click: () => setSelectedTherapist(therapist),
                }}
              >
                <Popup>
                  <div className="p-1 space-y-1 text-xs">
                    <div className="font-bold text-sm flex items-center gap-1.5">
                      <span>{therapist.name}</span>
                      <span className="text-[10px] bg-emerald-100 text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-400 px-1.5 py-0.5 rounded font-bold">
                        {therapist.tier_badge || 'Gold Master'}
                      </span>
                    </div>
                    <div className="text-slate-500 dark:text-slate-400">
                      ID: {therapist.id} • {therapist.gender}
                    </div>
                    <div className="font-mono text-slate-700 dark:text-slate-300">
                      📞 {therapist.phone}
                    </div>
                    <div className="text-amber-500 font-semibold">
                      ⭐ {therapist.rating} ({therapist.orders_completed} Pesanan Selesai)
                    </div>
                    <div className="text-[11px] pt-1 border-t border-slate-200 dark:border-slate-700">
                      Status:{' '}
                      <strong
                        className={
                          st === 'ONLINE'
                            ? 'text-emerald-600 dark:text-emerald-400'
                            : st === 'ON_DUTY_BUSY'
                            ? 'text-amber-600 dark:text-amber-400 font-bold'
                            : 'text-slate-500'
                        }
                      >
                        {st === 'ON_DUTY_BUSY' ? 'BERTUGAS (Menuju Lokasi / Sedang Melayani)' : st}
                      </strong>
                      {st === 'OFFLINE' && (
                        <span className="block text-[10px] text-slate-500 italic mt-0.5">
                          (Titik GPS Terakhir Tersimpan)
                        </span>
                      )}
                    </div>
                  </div>
                </Popup>
              </Marker>
            );
          })}

          {/* Active Orders Road Routing with actual customer GPS */}
          {activeOrdersWithRoutes.map((order) => {
            const matchedTherapist = therapists.find((t) => t.id === order.therapist_id);
            if (!matchedTherapist) return null;

            return (
              <ActiveOrderRouteComponent
                key={order.id}
                order={order}
                matchedTherapist={matchedTherapist}
              />
            );
          })}
        </MapContainer>
      );
    };
  },
  { ssr: false }
);

export const LiveFleetMap: React.FC<LiveFleetMapProps> = ({
  therapists,
  orders,
  isDarkMode = false,
}) => {
  const [selectedTherapist, setSelectedTherapist] = useState<Therapist | null>(null);
  const [filterStatus, setFilterStatus] = useState<'ALL' | 'ONLINE' | 'BUSY' | 'OFFLINE'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isClient, setIsClient] = useState(false);

  useEffect(() => {
    setIsClient(true);
  }, []);

  const activeOrdersWithRoutes = orders.filter(
    (o) =>
      (o.status === 'ACCEPTED_ON_THE_WAY' || o.status === 'ARRIVED_AT_LOCATION') &&
      o.therapist_id
  );

  // Auto calculate map center based on active order customer GPS or first online therapist GPS
  const defaultCenter: [number, number] = React.useMemo(() => {
    if (activeOrdersWithRoutes.length > 0) {
      const activeOrd = activeOrdersWithRoutes[0];
      return extractCustomerCoords(activeOrd);
    }
    const onlineTherapist = therapists.find((t) => (t.is_online || t.duty_status === 'ONLINE' || t.duty_status === 'ON_DUTY_BUSY') && t.latitude && t.longitude);
    if (onlineTherapist) {
      return [onlineTherapist.latitude, onlineTherapist.longitude];
    }
    return [-7.7956, 110.3695];
  }, [activeOrdersWithRoutes, therapists]);

  const filteredTherapists = therapists.filter((t) => {
    const matchesSearch =
      t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.phone.includes(searchQuery) ||
      t.id.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;
    const st = computeTherapistEffectiveStatus(t, orders);
    if (filterStatus === 'ALL') return true;
    if (filterStatus === 'ONLINE') return st === 'ONLINE';
    if (filterStatus === 'BUSY') return st === 'ON_DUTY_BUSY';
    if (filterStatus === 'OFFLINE') return st === 'OFFLINE';
    return true;
  });

  const onlineCount = therapists.filter((t) => computeTherapistEffectiveStatus(t, orders) === 'ONLINE').length;
  const busyCount = therapists.filter((t) => computeTherapistEffectiveStatus(t, orders) === 'ON_DUTY_BUSY').length;
  const offlineCount = therapists.filter((t) => computeTherapistEffectiveStatus(t, orders) === 'OFFLINE').length;

  return (
    <div className="h-[calc(100vh-8rem)] flex flex-col lg:flex-row gap-4 overflow-hidden">
      {/* Left Map Viewport */}
      <div className="flex-1 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden relative flex flex-col shadow-sm">
        {/* Floating Top Controls */}
        <div className="absolute top-4 left-4 right-4 z-[400] flex flex-wrap items-center justify-between gap-3 pointer-events-none">
          <div className="flex items-center gap-2 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md p-1.5 rounded-2xl border border-slate-200 dark:border-slate-800 pointer-events-auto shadow-xl ml-12 sm:ml-14">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Cari mitra / ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 pr-3 py-1.5 bg-slate-100 dark:bg-slate-800 text-xs text-slate-900 dark:text-white rounded-xl placeholder:text-slate-400 dark:placeholder:text-slate-500 border border-slate-200 dark:border-slate-700/60 focus:outline-none focus:border-emerald-500 w-36 sm:w-44"
              />
            </div>

            <div className="h-6 w-[1px] bg-slate-200 dark:bg-slate-800" />

            <div className="flex items-center gap-1">
              <button
                onClick={() => setFilterStatus('ALL')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition ${
                  filterStatus === 'ALL'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                Semua ({therapists.length})
              </button>
              <button
                onClick={() => setFilterStatus('ONLINE')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition ${
                  filterStatus === 'ONLINE'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                🟢 Online ({onlineCount})
              </button>
              <button
                onClick={() => setFilterStatus('BUSY')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition ${
                  filterStatus === 'BUSY'
                    ? 'bg-amber-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                🟠 Bertugas ({busyCount})
              </button>
              <button
                onClick={() => setFilterStatus('OFFLINE')}
                className={`px-2.5 py-1 rounded-xl text-xs font-semibold transition ${
                  filterStatus === 'OFFLINE'
                    ? 'bg-slate-700 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800'
                }`}
              >
                ⚪ Offline ({offlineCount})
              </button>
            </div>
          </div>

          {/* Quick Realtime Indicator */}
          <div className="bg-white/95 dark:bg-slate-900/95 backdrop-blur-md px-4 py-2 rounded-2xl border border-slate-200 dark:border-slate-800 pointer-events-auto shadow-xl flex items-center gap-3 text-xs">
            <div className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-full bg-emerald-500 animate-ping" />
              <span className="font-bold text-slate-800 dark:text-white">Live Fleet GPS</span>
            </div>
            <div className="text-slate-300 dark:text-slate-700">•</div>
            <div className="text-slate-600 dark:text-slate-300">
              <span className="font-bold text-amber-500">{activeOrdersWithRoutes.length}</span> Motor Di Jalan
            </div>
          </div>
        </div>

        {/* Map Canvas */}
        {isClient ? (
          <div className="w-full h-full">
            <MapInner
              therapists={filteredTherapists}
              orders={orders}
              isDarkMode={isDarkMode}
              selectedTherapist={selectedTherapist}
              setSelectedTherapist={setSelectedTherapist}
              defaultCenter={defaultCenter}
            />
          </div>
        ) : (
          <div className="flex-1 flex items-center justify-center text-slate-400 text-xs">
            <span>Memuat Peta Armada Realtime...</span>
          </div>
        )}
      </div>

      {/* Right Telemetry Drawer */}
      <div className="w-full lg:w-80 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 flex flex-col justify-between overflow-y-auto shadow-sm">
        {selectedTherapist ? (
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
              <div className="flex items-center gap-3">
                <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-emerald-600 to-emerald-400 flex items-center justify-center text-white font-bold text-base shadow-md">
                  {selectedTherapist.name.substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <h4 className="text-sm font-bold text-slate-900 dark:text-white">{selectedTherapist.name}</h4>
                  <p className="text-xs text-slate-500 dark:text-slate-400">{selectedTherapist.id}</p>
                </div>
              </div>

              <Badge
                variant={
                  computeTherapistEffectiveStatus(selectedTherapist, orders) === 'ONLINE'
                    ? 'emerald'
                    : computeTherapistEffectiveStatus(selectedTherapist, orders) === 'ON_DUTY_BUSY'
                    ? 'amber'
                    : 'slate'
                }
                size="sm"
                pulse={computeTherapistEffectiveStatus(selectedTherapist, orders) === 'ONLINE' || computeTherapistEffectiveStatus(selectedTherapist, orders) === 'ON_DUTY_BUSY'}
              >
                {computeTherapistEffectiveStatus(selectedTherapist, orders) === 'ONLINE'
                  ? 'Online (Siap Menerima Order)'
                  : computeTherapistEffectiveStatus(selectedTherapist, orders) === 'ON_DUTY_BUSY'
                  ? 'Bertugas (Menuju Lokasi / Sedang Melayani)'
                  : 'Offline (Last GPS)'}
              </Badge>
            </div>

            {/* Quick Metrics */}
            <div className="grid grid-cols-2 gap-2">
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800">
                <div className="text-[11px] text-slate-500 dark:text-slate-400">Rating Kepuasan</div>
                <div className="text-sm font-bold text-amber-500 flex items-center gap-1 mt-0.5">
                  <Star className="w-4 h-4 fill-amber-500" />
                  <span>{selectedTherapist.rating}</span>
                  <span className="text-[10px] text-slate-400">({selectedTherapist.review_count})</span>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950/40 border border-slate-200 dark:border-slate-800">
                <div className="text-[11px] text-slate-500 dark:text-slate-400">Total Order Tuntas</div>
                <div className="text-sm font-bold text-slate-900 dark:text-white mt-0.5">
                  {selectedTherapist.orders_completed} Selesai
                </div>
              </div>
            </div>

            {/* Telemetry Details */}
            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-transparent">
                <span className="text-slate-500 dark:text-slate-400">Nomor Telepon:</span>
                <span className="font-mono font-semibold text-slate-900 dark:text-white">{selectedTherapist.phone}</span>
              </div>
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-transparent">
                <span className="text-slate-500 dark:text-slate-400">Radius Jangkauan:</span>
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">{selectedTherapist.max_radius_km || 10} KM</span>
              </div>
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-transparent">
                <span className="text-slate-500 dark:text-slate-400">Saldo Dompet:</span>
                <span className="font-semibold text-slate-900 dark:text-white">Rp {Number(selectedTherapist.wallet_balance || 0).toLocaleString('id-ID')}</span>
              </div>
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-800/40 border border-slate-100 dark:border-transparent">
                <span className="text-slate-500 dark:text-slate-400">Saldo Deposit:</span>
                <span className="font-semibold text-emerald-600 dark:text-emerald-400">Rp {Number(selectedTherapist.deposit_balance || 100000).toLocaleString('id-ID')}</span>
              </div>
              <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-transparent">
                <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-1">
                  <span>GPS Koordinat Terakhir:</span>
                  <span className="font-mono font-semibold text-emerald-600 dark:text-emerald-400">
                    {selectedTherapist.latitude?.toFixed(4)}, {selectedTherapist.longitude?.toFixed(4)}
                  </span>
                </div>
                {computeTherapistEffectiveStatus(selectedTherapist, orders) === 'OFFLINE' && (
                  <div className="text-[10px] text-slate-400 flex items-center gap-1">
                    <Clock className="w-3 h-3 text-slate-400" />
                    <span>Terakhir aktif tersimpan di database Supabase</span>
                  </div>
                )}
              </div>
            </div>

            {/* Certifications */}
            <div>
              <div className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 mb-1.5">Sertifikasi Resmi:</div>
              <div className="flex flex-wrap gap-1.5">
                {(selectedTherapist.certifications || ['Sertifikasi BNSP', 'Pijat Tradisional']).map((cert, idx) => (
                  <span
                    key={idx}
                    className="px-2 py-0.5 rounded-md bg-emerald-50 dark:bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 text-[10px] font-medium border border-emerald-200 dark:border-emerald-500/20"
                  >
                    {cert}
                  </span>
                ))}
              </div>
            </div>
          </div>
        ) : (
          <div className="h-full flex flex-col items-center justify-center text-center p-6 text-slate-400">
            <Compass className="w-12 h-12 mb-2 text-slate-300 dark:text-slate-600 stroke-[1.5]" />
            <p className="text-xs font-semibold text-slate-700 dark:text-slate-300">Pilih Mitra di Peta</p>
            <p className="text-[11px] text-slate-500 mt-1 max-w-[200px]">
              Klik pada salah satu marker motor (🟢 Online, 🟠 Bertugas, atau ⚪ Offline) untuk memantau telemetri GPS dan status tugas mitra.
            </p>
          </div>
        )}

        <div className="pt-4 border-t border-slate-100 dark:border-slate-800 text-[11px] text-slate-400 flex items-center justify-between">
          <span>Engine: Realtime Vector Map</span>
          <span className="text-emerald-600 dark:text-emerald-400 font-medium">GPS Auto-Sync</span>
        </div>
      </div>
    </div>
  );
};
