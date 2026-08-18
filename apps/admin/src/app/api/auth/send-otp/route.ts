import { NextRequest, NextResponse } from 'next/server';
import { supabase, getPlatformSettings } from '@/lib/supabase';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    let { phone, user_type = 'CUSTOMER' } = body; // 'CUSTOMER' or 'MITRA'

    if (!phone) {
      return NextResponse.json({ error: 'Nomor WhatsApp wajib diisi' }, { status: 400 });
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

    // 1. Generate 4-Digit Secure OTP
    const otpCode = Math.floor(1000 + Math.random() * 9000).toString();
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString(); // 5 mins

    // 2. Fetch Fonnte Token from Supabase settings
    const settings = await getPlatformSettings();
    const fonnteToken = settings.fonnte_token || 'G7i1MwMXPn2pKoSd9HiF';
    const isFonnteEnabled = settings.fonnte_enabled !== 'false';

    // 3. Save OTP in Supabase (upsert or insert)
    try {
      await supabase.from('otp_verifications').insert([
        {
          phone,
          otp_code: otpCode,
          expires_at: expiresAt,
          is_verified: false,
          user_type,
        },
      ]);
    } catch (_: any) {}

    // 4. Send WhatsApp message via Fonnte API
    let sentStatus = false;
    let fonnteResponse: any = null;

    if (fonnteToken && isFonnteEnabled) {
      const message = `*Kode Verifikasi PijatIn (OTP)* 💆‍♂️\n\nKode OTP Anda adalah: *${otpCode}*\n\nJangan bagikan kode ini kepada siapa pun demi keamanan akun Anda. Berlaku selama 5 menit.`;

      const fonnteRes = await fetch('https://api.fonnte.com/send', {
        method: 'POST',
        headers: {
          Authorization: fonnteToken,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          target: phone,
          message,
          countryCode: '62',
        }),
      });

      fonnteResponse = await fonnteRes.json();
      sentStatus = fonnteResponse.status === true;
    }

    return NextResponse.json({
      success: true,
      phone,
      otp_code: otpCode, // sent for development inspection/fallback
      fonnte_status: sentStatus,
      message: sentStatus ? 'Kode OTP berhasil dikirim ke WhatsApp Anda' : 'Kode OTP siap diverifikasi',
      fonnte_response: fonnteResponse,
    });
  } catch (error: any) {
    console.error('Error sending OTP:', error);
    return NextResponse.json({ error: error?.message || 'Gagal mengirim kode OTP' }, { status: 500 });
  }
}
