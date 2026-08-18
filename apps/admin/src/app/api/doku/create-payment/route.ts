import { NextRequest, NextResponse } from 'next/server';
import crypto from 'crypto';
import { supabase, getPlatformSettings } from '@/lib/supabase';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const {
      order_id,
      amount,
      service_name,
      customer_name,
      customer_phone,
      customer_id,
      type = 'ORDER', // 'ORDER' or 'DEPOSIT_TOPUP'
      therapist_id,
    } = body;

    if (!amount || amount <= 0) {
      return NextResponse.json({ error: 'Nominal pembayaran tidak valid' }, { status: 400 });
    }

    // 1. Fetch live DOKU credentials from Supabase platform_settings
    const settings = await getPlatformSettings();
    const isProduction = settings.doku_is_production === 'true';
    const clientId = settings.doku_client_id || 'BRN-0242-1787022128265';
    const secretKey = settings.doku_secret_key || '';
    const isEnabled = settings.doku_enabled !== 'false';

    const invoiceNumber = `${type === 'DEPOSIT_TOPUP' ? 'TOPUP' : 'INV'}-${Date.now()}-${(order_id || 'TEMP').slice(-4)}`;
    const baseUrl = settings.doku_is_production === 'false' ? 'https://api-sandbox.doku.com' : 'https://api.doku.com';
    const targetPath = '/checkout/v1/payment';

    // 2. Build DOKU Checkout Payload
    const dokuPayload = {
      order: {
        amount: Number(amount),
        invoice_number: invoiceNumber,
        currency: 'IDR',
        callback_url: `${req.nextUrl.origin}/api/doku/notify?type=${type}`,
        auto_redirect: true,
      },
      payment: {
        payment_due_date: 60, // 60 minutes expiry
      },
      customer: {
        id: customer_id || therapist_id || 'CUST-PIJATIN',
        name: customer_name || 'Pelanggan MassaGo',
        phone: customer_phone || '+6281234567890',
      },
      additional_info: {
        order_id: order_id || '',
        therapist_id: therapist_id || '',
        type,
        service_name: service_name || 'Layanan MassaGo',
      },
    };

    // If Secret Key is available, attempt real DOKU API Call
    if (secretKey && isEnabled) {
      const rawBody = JSON.stringify(dokuPayload);
      const requestId = `REQ-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
      const requestTimestamp = new Date().toISOString().slice(0, 19) + 'Z';

      // Generate SHA-256 Digest
      const digest = crypto.createHash('sha256').update(rawBody, 'utf-8').digest('base64');

      // Generate Component Signature
      const signatureComponent = `Client-Id:${clientId}\nRequest-Id:${requestId}\nRequest-Timestamp:${requestTimestamp}\nRequest-Target:${targetPath}\nDigest:${digest}`;
      const signature = crypto
        .createHmac('sha256', secretKey)
        .update(signatureComponent)
        .digest('base64');

      const dokuResponse = await fetch(`${baseUrl}${targetPath}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Client-Id': clientId,
          'Request-Id': requestId,
          'Request-Timestamp': requestTimestamp,
          'Signature': `HMACSHA256=${signature}`,
        },
        body: rawBody,
      });

      const dokuData = await dokuResponse.json();

      if (dokuResponse.ok && dokuData?.response?.payment?.url) {
        const paymentUrl = dokuData.response.payment.url;
        const invoiceId = dokuData.response.payment.token || invoiceNumber;

        // Update Order in Supabase if order_id exists
        if (order_id) {
          await supabase
            .from('orders')
            .update({
              payment_invoice_url: paymentUrl,
              doku_invoice_id: invoiceId,
              payment_status: 'UNPAID',
            })
            .eq('id', order_id);
        }

        return NextResponse.json({
          success: true,
          mode: isProduction ? 'PRODUCTION' : 'SANDBOX',
          payment_url: paymentUrl,
          invoice_number: invoiceNumber,
          doku_response: dokuData,
        });
      }
    }

    // 3. Fallback / Simulator Mode (Jika secret key belum diinput atau sandbox)
    const simulatedPaymentUrl = `${req.nextUrl.origin}/checkout/doku-simulator?invoice=${invoiceNumber}&amount=${amount}&service=${encodeURIComponent(service_name || 'Layanan MassaGo')}&type=${type}&order_id=${order_id || ''}&therapist_id=${therapist_id || ''}`;

    if (order_id) {
      await supabase
        .from('orders')
        .update({
          payment_invoice_url: simulatedPaymentUrl,
          doku_invoice_id: invoiceNumber,
          payment_status: 'UNPAID',
        })
        .eq('id', order_id);
    }

    return NextResponse.json({
      success: true,
      mode: 'SIMULATOR',
      payment_url: simulatedPaymentUrl,
      invoice_number: invoiceNumber,
      message: 'Checkout DOKU siap digunakan.',
    });
  } catch (error: any) {
    console.error('Error creating DOKU payment:', error);
    return NextResponse.json(
      { error: error?.message || 'Gagal membuat sesi pembayaran DOKU' },
      { status: 500 }
    );
  }
}
