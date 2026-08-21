export type DutyStatus = 'ONLINE' | 'OFFLINE' | 'ON_DUTY_BUSY';

export interface Therapist {
  id: string;
  name: string;
  phone: string;
  gender: string;
  rating: number;
  review_count: number;
  orders_completed: number;
  wallet_balance: number;
  deposit_balance: number;
  is_online: boolean;
  duty_status: DutyStatus;
  max_radius_km: number;
  preferred_client_gender: string;
  tier_badge: string;
  latitude: number;
  longitude: number;
  certifications?: string[];
  created_at?: string;
  // UI helpers
  avatar_url?: string;
  is_active?: boolean;
}

export type OrderStatus = 
  | 'PENDING'
  | 'ACCEPTED_ON_THE_WAY'
  | 'ARRIVED_AT_LOCATION'
  | 'SANITATION_AND_PREP'
  | 'TREATMENT_IN_PROGRESS'
  | 'COMPLETED_PAYMENT'
  | 'REVIEW_SUBMITTED'
  | 'CANCELLED';

export interface Order {
  id: string;
  customer_id?: string;
  therapist_id?: string;
  service_name: string;
  duration_minutes: number;
  total_price: number;
  status: string;
  customer_name?: string;
  customer_phone?: string;
  address?: string;
  gender_preference?: string;
  created_at: number | string;
  customer_lat?: number;
  customer_lng?: number;
  latitude?: number;
  longitude?: number;
  commission_rate?: number;
  cancellation_reason?: string;
  notes?: string;
  // Join helpers
  therapist?: Therapist;
}

export interface ServicePackage {
  id: string;
  name: string;
  category: string;
  short_description: string;
  full_description: string;
  benefits: string[];
  price_60: number;
  price_90: number;
  price_120: number;
  icon_emoji: string;
  orders_count: number;
  created_at?: string;
}

export interface PromoVoucher {
  code: string;
  title: string;
  description: string;
  discount_percent: number;
  discount_flat: number;
  max_discount: number;
  min_spend: number;
  is_active: boolean;
  created_at?: string;
}

export interface SosAlert {
  id: string;
  sender_type: 'CUSTOMER' | 'THERAPIST';
  sender_id: string;
  sender_name: string;
  sender_phone: string;
  latitude: number;
  longitude: number;
  timestamp: number;
  status: 'ACTIVE' | 'INVESTIGATING' | 'RESOLVED';
  order_id?: string;
  notes?: string;
}

export interface KycApplication {
  id: string;
  name: string;
  phone: string;
  gender: string;
  ktp_number: string;
  skck_status: string;
  bnsp_cert_name: string;
  experience_years: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  submitted_at: string;
}

export interface PayoutRequest {
  id: string;
  therapist_id: string;
  therapist_name: string;
  bank_name: string;
  account_number: string;
  account_holder: string;
  amount: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requested_at: string;
}

export interface Review {
  id: string;
  order_id: string;
  reviewer_type: 'CUSTOMER' | 'THERAPIST';
  reviewer_id: string;
  target_id: string;
  rating: number;
  tags?: string[];
  review_text?: string;
  created_at?: string;
}

export interface PlatformBankAccount {
  id: string;
  bank_name: string;
  account_number: string;
  account_holder: string;
  is_active: boolean;
  created_at?: string;
}

export interface PlatformSetting {
  key: string;
  value: string;
  description?: string;
  updated_at?: string;
}

export interface CustomerProfile {
  id: string;
  phone: string;
  full_name: string;
  role: string;
  avatar_url?: string;
  created_at?: string;
}

export type ActiveTab = 
  | 'dashboard'
  | 'godview'
  | 'orders'
  | 'customers'
  | 'mitra'
  | 'kyc'
  | 'catalog'
  | 'vouchers'
  | 'sos'
  | 'finance'
  | 'payments';


