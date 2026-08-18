import { NextRequest, NextResponse } from 'next/server';
import { supabase } from '@/lib/supabase';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    let { phone, otp_code, user_type = 'CUSTOMER' } = body;

    if (!phone || !otp_code) {
      return NextResponse.json({ error: 'Nomor WhatsApp dan Kode OTP wajib diisi' }, { status: 400 });
    }

    // Format phone to 628xxx format
    phone = phone.replace(/[^0-9]/g, '');
    if (phone.startsWith('0')) {
      phone = '62' + phone.substring(1);
    } else if (phone.startsWith('8')) {
      phone = '62' + phone;
    } else if (!phone.startsWith('62')) {
      phone = '62' + phone;
    }

    // Bypass for universal testing code 1234 or 8888
    const isMasterOtp = otp_code === '1234' || otp_code === '8888';

    if (!isMasterOtp) {
      // Check Supabase OTP record
      const { data: records, error } = await supabase
        .from('otp_verifications')
        .select('*')
        .eq('phone', phone)
        .eq('otp_code', otp_code)
        .order('created_at', { ascending: false })
        .limit(1);

      if (error || !records || records.length === 0) {
        return NextResponse.json({ error: 'Kode OTP salah atau tidak ditemukan' }, { status: 400 });
      }

      const otpRecord = records[0];
      if (new Date(otpRecord.expires_at).getTime() < Date.now()) {
        return NextResponse.json({ error: 'Kode OTP telah kedaluwarsa, silakan minta kode baru' }, { status: 400 });
      }

      // Mark verified
      await supabase
        .from('otp_verifications')
        .update({ is_verified: true })
        .eq('id', otpRecord.id);
    }

    // Check if user is already registered in DB
    let isRegistered = false;
    let userData: any = null;

    if (user_type === 'CUSTOMER') {
      const cleanPhone = phone.replace(/[^0-9]/g, '');
      const localPhone = cleanPhone.startsWith('62') ? '0' + cleanPhone.substring(2) : cleanPhone;
      const intlPhone = cleanPhone.startsWith('62') ? '+' + cleanPhone : '+62' + cleanPhone;

      const { data: customers } = await supabase
        .from('profiles')
        .select('*')
        .or(`phone.eq.${cleanPhone},phone.eq.${localPhone},phone.eq.${intlPhone}`)
        .limit(1);

      if (customers && customers.length > 0) {
        isRegistered = true;
        userData = customers[0];
      }
    } else {
      // MITRA
      const { data: therapists } = await supabase
        .from('therapists')
        .select('*')
        .eq('phone', phone)
        .limit(1);

      if (therapists && therapists.length > 0) {
        isRegistered = true;
        userData = therapists[0];
      }
    }

    return NextResponse.json({
      success: true,
      phone,
      is_registered: isRegistered,
      user_data: userData,
      message: 'Verifikasi nomor WhatsApp berhasil',
    });
  } catch (error: any) {
    console.error('Error verifying OTP:', error);
    return NextResponse.json({ error: error?.message || 'Gagal memverifikasi OTP' }, { status: 500 });
  }
}
