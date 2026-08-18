import { createClient } from '@supabase/supabase-js';
import { Therapist, Order, ServicePackage, PromoVoucher, PlatformBankAccount, PlatformSetting, CustomerProfile } from '../types';

export const SUPABASE_URL = 'https://jrwkmedrrwvomyljdkpw.supabase.co';
export const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impyd2ttZWRycnd2b215bGpka3B3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTcxNzQsImV4cCI6MjEwMjQ5MzE3NH0.UiN6JvJt23ds-3eID9J6wOtEt3pg4-farSwQIliPzuw';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export async function getCustomers(): Promise<CustomerProfile[]> {
  try {
    const { data, error } = await supabase
      .from('profiles')
      .select('*')
      .order('created_at', { ascending: false });

    if (error) throw error;
    return data || [];
  } catch (err) {
    console.error('Error fetching customers from Supabase:', err);
    return [];
  }
}

// Canonical Standard Bank Accounts
export const CANONICAL_BANK_ACCOUNTS: PlatformBankAccount[] = [
  {
    id: 'BANK-BCA',
    bank_name: 'Bank Central Asia (BCA)',
    account_number: '8420891234',
    account_holder: 'PT PIJATIN INDONESIA SEJAHTERA',
    is_active: true,
  },
  {
    id: 'BANK-MANDIRI',
    bank_name: 'Bank Mandiri',
    account_number: '1370019283746',
    account_holder: 'PT PIJATIN INDONESIA SEJAHTERA',
    is_active: true,
  },
  {
    id: 'BANK-BRI',
    bank_name: 'Bank Rakyat Indonesia (BRI)',
    account_number: '034101002345538',
    account_holder: 'PT PIJATIN INDONESIA SEJAHTERA',
    is_active: true,
  },
];

// Canonical Platform Settings
export const CANONICAL_SETTINGS: Record<string, string> = {
  qris_image_url: '/qris-pijatin.png',
  qris_merchant_name: 'PIJATIN INDONESIA',
  qris_nmid: 'ID1020030040050',
  admin_whatsapp: '+6281234567890',
  platform_commission_percent: '20',
};

// Canonical Standard Service Catalog
export const CANONICAL_SERVICE_PACKAGES: ServicePackage[] = [
  {
    id: 'SRV-TRAD',
    name: 'Pijat Tradisional Jawa',
    category: 'Tradisional',
    short_description: 'Pijatan urut seluruh tubuh dengan teknik tradisional warisan leluhur.',
    full_description: 'Pijatan menyeluruh untuk melancarkan sirkulasi darah dan melepas otot kaku.',
    benefits: ['Melancarkan peredaran darah', 'Meredakan pegal linu', 'Membantu tidur nyenyak'],
    price_60: 120000,
    price_90: 160000,
    price_120: 210000,
    icon_emoji: '💆‍♂️',
    orders_count: 0,
  },
  {
    id: 'SRV-REFL',
    name: 'Refleksi Kaki & Akupresur',
    category: 'Refleksi',
    short_description: 'Titik akupresur telapak kaki dan tangan untuk memulihkan vitalitas.',
    full_description: 'Terapi saraf titik refleksi organ tubuh untuk melancarkan metabolisme tubuh.',
    benefits: ['Meredakan ketegangan kaki', 'Melancarkan metabolisme', 'Mencegah kram'],
    price_60: 110000,
    price_90: 150000,
    price_120: 190000,
    icon_emoji: '🦶',
    orders_count: 0,
  },
  {
    id: 'SRV-DEEP',
    name: 'Deep Tissue & Sport Massage',
    category: 'Kebugaran',
    short_description: 'Tekanan intensif untuk simpul otot kaku setelah olahraga berat.',
    full_description: 'Teknik pijat lapisan jaringan otot dalam untuk mengurai asam laktat.',
    benefits: ['Memecah simpul otot kaku', 'Mempercepat pemulihan', 'Meningkatkan kelenturan'],
    price_60: 150000,
    price_90: 210000,
    price_120: 260000,
    icon_emoji: '💪',
    orders_count: 0,
  },
  {
    id: 'SRV-SCRUB',
    name: 'Lulur & Body Scrub Spa',
    category: 'Spa & Kulit',
    short_description: 'Pijat relaksasi dipadu scrub rempah organik untuk kulit cerah & halus.',
    full_description: 'Perawatan spa menyeluruh diawali pijatan lembut dan lulur rempah bengkoang/kopi.',
    benefits: ['Mengangkat sel kulit mati', 'Mencerahkan & melembutkan', 'Sensasi spa di rumah'],
    price_60: 0,
    price_90: 200000,
    price_120: 245000,
    icon_emoji: '✨',
    orders_count: 0,
  },
  {
    id: 'SRV-BEKAM',
    name: 'Bekam & Kerokan Higienis',
    category: 'Kesehatan',
    short_description: 'Pelepas masuk angin dan letih dengan peralatan steril higienis.',
    full_description: 'Terapi kerokan halus atau bekam kering dengan alat steril 1x pakai.',
    benefits: ['Meredakan masuk angin', 'Melonggarkan pernapasan', 'Peralatan 100% steril'],
    price_60: 135000,
    price_90: 185000,
    price_120: 0,
    icon_emoji: '🍃',
    orders_count: 0,
  },
  {
    id: 'SRV-PRENATAL',
    name: 'Pijat Relaksasi Ibu Hamil',
    category: 'Khusus',
    short_description: 'Pijatan lembut khusus ibu hamil oleh terapis bersertifikasi resmi prenatal.',
    full_description: 'Posisi menyamping yang aman dan nyaman untuk meredakan nyeri pinggang & pegal kaki.',
    benefits: ['Certified Prenatal Therapist', 'Meredakan nyeri panggul', 'Menenangkan calon ibu'],
    price_60: 175000,
    price_90: 230000,
    price_120: 0,
    icon_emoji: '🤰',
    orders_count: 0,
  },
];

// Canonical Standard Promo Vouchers
export const CANONICAL_PROMO_VOUCHERS: PromoVoucher[] = [
  {
    code: 'PIJATINBARU',
    title: 'Diskon 30% Pengguna Baru PijatIn',
    description: 'Potongan s/d Rp35.000 untuk pesanan pertama Anda',
    discount_percent: 30,
    discount_flat: 0,
    max_discount: 35000,
    min_spend: 100000,
    is_active: true,
  },
  {
    code: 'HEMATWEEKEND',
    title: 'Potongan Langsung Rp 20.000',
    description: 'Spesial relaksasi akhir pekan minimal order Rp150.000',
    discount_percent: 0,
    discount_flat: 20000,
    max_discount: 20000,
    min_spend: 150000,
    is_active: true,
  },
  {
    code: 'SPALUXURY',
    title: 'Diskon Rp 50.000 Paket Spa & Scrub',
    description: 'Hemat Rp50.000 untuk paket perawatan 120 menit',
    discount_percent: 0,
    discount_flat: 50000,
    max_discount: 50000,
    min_spend: 200000,
    is_active: true,
  },
];

// Live Services Catalog API
export async function getServicePackages(): Promise<ServicePackage[]> {
  try {
    const { data, error } = await supabase
      .from('service_packages')
      .select('*')
      .order('orders_count', { ascending: false });

    if (error || !data || data.length === 0) {
      return CANONICAL_SERVICE_PACKAGES;
    }
    return data as ServicePackage[];
  } catch (err) {
    return CANONICAL_SERVICE_PACKAGES;
  }
}

export async function upsertServicePackage(pkg: Partial<ServicePackage>): Promise<boolean> {
  try {
    const payload = {
      id: pkg.id,
      name: pkg.name,
      category: pkg.category || 'Tradisional',
      short_description: pkg.short_description || '',
      full_description: pkg.full_description || '',
      benefits: pkg.benefits || [],
      price_60: Number(pkg.price_60) || 0,
      price_90: Number(pkg.price_90) || 0,
      price_120: Number(pkg.price_120) || 0,
      icon_emoji: pkg.icon_emoji || '💆‍♂️',
      orders_count: Number(pkg.orders_count) || 0,
    };
    const { error } = await supabase
      .from('service_packages')
      .upsert([payload], { onConflict: 'id' });
    if (error) {
      console.error('Supabase upsertServicePackage error:', error);
      return false;
    }
    return true;
  } catch (err) {
    console.error('Error upserting service package:', err);
    return false;
  }
}

export async function deleteServicePackage(id: string): Promise<boolean> {
  try {
    const { error } = await supabase
      .from('service_packages')
      .delete()
      .eq('id', id);
    if (error) {
      console.error('Supabase deleteServicePackage error:', error);
      return false;
    }
    return true;
  } catch (err) {
    console.error('Error deleting service package:', err);
    return false;
  }
}

// Live Promo Vouchers API
export async function getPromoVouchers(): Promise<PromoVoucher[]> {
  try {
    const { data, error } = await supabase
      .from('promo_vouchers')
      .select('*')
      .order('created_at', { ascending: false });

    if (error || !data || data.length === 0) {
      return CANONICAL_PROMO_VOUCHERS;
    }
    return data as PromoVoucher[];
  } catch (err) {
    return CANONICAL_PROMO_VOUCHERS;
  }
}

export async function upsertPromoVoucher(voucher: Partial<PromoVoucher>): Promise<boolean> {
  try {
    const payload = {
      code: voucher.code?.toUpperCase().trim(),
      title: voucher.title,
      description: voucher.description || '',
      discount_percent: Number(voucher.discount_percent) || 0,
      discount_flat: Number(voucher.discount_flat) || 0,
      max_discount: Number(voucher.max_discount) || 50000,
      min_spend: Number(voucher.min_spend) || 0,
      is_active: voucher.is_active !== undefined ? voucher.is_active : true,
    };
    const { error } = await supabase
      .from('promo_vouchers')
      .upsert([payload], { onConflict: 'code' });
    if (error) {
      console.error('Supabase upsertPromoVoucher error:', error);
      return false;
    }
    return true;
  } catch (err) {
    console.error('Error upserting voucher:', err);
    return false;
  }
}

export async function deletePromoVoucher(code: string): Promise<boolean> {
  try {
    const { error } = await supabase
      .from('promo_vouchers')
      .delete()
      .eq('code', code);
    if (error) {
      console.error('Supabase deletePromoVoucher error:', error);
      return false;
    }
    return true;
  } catch (err) {
    console.error('Error deleting voucher:', err);
    return false;
  }
}

// Live Therapists API
export async function getTherapists(): Promise<Therapist[]> {
  try {
    const { data, error } = await supabase
      .from('therapists')
      .select('*')
      .order('rating', { ascending: false });

    if (error || !data) return [];
    return data as Therapist[];
  } catch (err) {
    console.error('Error fetching therapists:', err);
    return [];
  }
}

export async function updateTherapist(id: string, updates: Partial<Therapist>): Promise<boolean> {
  try {
    const { error } = await supabase
      .from('therapists')
      .update(updates)
      .eq('id', id);
    return !error;
  } catch (err) {
    console.error('Error updating therapist:', err);
    return false;
  }
}

// Live Orders API
export async function getOrders(): Promise<Order[]> {
  try {
    const { data, error } = await supabase
      .from('orders')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(100);

    if (error || !data) return [];
    return data as Order[];
  } catch (err) {
    console.error('Error fetching orders:', err);
    return [];
  }
}

export async function updateOrderStatus(orderId: string, status: string, therapistId?: string): Promise<boolean> {
  try {
    const updates: Record<string, any> = { status };
    if (therapistId) updates.therapist_id = therapistId;

    const { error } = await supabase
      .from('orders')
      .update(updates)
      .eq('id', orderId);
    return !error;
  } catch (err) {
    console.error('Error updating order status:', err);
    return false;
  }
}

// Bank Accounts API
export async function getBankAccounts(): Promise<PlatformBankAccount[]> {
  try {
    const { data, error } = await supabase
      .from('platform_bank_accounts')
      .select('*')
      .order('created_at', { ascending: true });

    if (error || !data || data.length === 0) return CANONICAL_BANK_ACCOUNTS;
    return data as PlatformBankAccount[];
  } catch (err) {
    console.error('Error fetching bank accounts:', err);
    return CANONICAL_BANK_ACCOUNTS;
  }
}

export async function saveBankAccount(bank: Partial<PlatformBankAccount>): Promise<boolean> {
  try {
    const id = bank.id || `BANK-${Date.now()}`;
    const payload = {
      id,
      bank_name: bank.bank_name || '',
      account_number: bank.account_number || '',
      account_holder: bank.account_holder || '',
      is_active: bank.is_active !== undefined ? bank.is_active : true,
    };

    const { error } = await supabase
      .from('platform_bank_accounts')
      .upsert(payload, { onConflict: 'id' });
    return !error;
  } catch (err) {
    console.error('Error saving bank account:', err);
    return false;
  }
}

export async function deleteBankAccount(id: string): Promise<boolean> {
  try {
    const { error } = await supabase
      .from('platform_bank_accounts')
      .delete()
      .eq('id', id);
    return !error;
  } catch (err) {
    console.error('Error deleting bank account:', err);
    return false;
  }
}

// Platform Settings API
export async function getPlatformSettings(): Promise<Record<string, string>> {
  try {
    const { data, error } = await supabase
      .from('platform_settings')
      .select('*');

    if (error || !data || data.length === 0) return CANONICAL_SETTINGS;
    const settingsMap: Record<string, string> = { ...CANONICAL_SETTINGS };
    data.forEach((item: { key: string; value: string }) => {
      settingsMap[item.key] = item.value;
    });
    return settingsMap;
  } catch (err) {
    console.error('Error fetching platform settings:', err);
    return CANONICAL_SETTINGS;
  }
}

export async function updatePlatformSetting(key: string, value: string, description?: string): Promise<boolean> {
  try {
    const payload: { key: string; value: string; description?: string; updated_at: string } = {
      key,
      value,
      updated_at: new Date().toISOString(),
    };
    if (description) payload.description = description;

    const { error } = await supabase
      .from('platform_settings')
      .upsert(payload, { onConflict: 'key' });
    return !error;
  } catch (err) {
    console.error('Error updating platform setting:', err);
    return false;
  }
}

