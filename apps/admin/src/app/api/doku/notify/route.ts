import { NextRequest, NextResponse } from 'next/server';
import { supabase } from '@/lib/supabase';

export async function GET() {
  return NextResponse.json({
    status: 'ACTIVE',
    service: 'PijatIn DOKU Webhook Notification Listener',
    timestamp: new Date().toISOString(),
    guide: 'Gunakan URL POST ini pada menu Settings > Notifications di Dashboard DOKU Anda.',
  });
}

export async function POST(req: NextRequest) {
  try {
    const rawBody = await req.json();
    console.log('[DOKU Webhook] Received notification:', JSON.stringify(rawBody));

    const invoiceNumber = rawBody?.order?.invoice_number || rawBody?.invoice_number;
    const transactionStatus = rawBody?.transaction?.status || rawBody?.status || 'SUCCESS';
    const amount = Number(rawBody?.order?.amount || rawBody?.amount || 0);
    const additionalInfo = rawBody?.additional_info || {};

    const type = additionalInfo.type || (invoiceNumber?.startsWith('TOPUP') ? 'DEPOSIT_TOPUP' : 'ORDER');
    const orderId = additionalInfo.order_id;
    const therapistId = additionalInfo.therapist_id;

    const isSuccess = transactionStatus === 'SUCCESS' || transactionStatus === 'PAID';

    if (isSuccess) {
      if (type === 'ORDER' && orderId) {
        // 1. Mark Order as PAID & trigger dispatch
        await supabase
          .from('orders')
          .update({
            payment_status: 'PAID',
            status: 'ACCEPTED_ON_THE_WAY',
          })
          .eq('id', orderId);

        console.log(`[DOKU Webhook] Order ${orderId} marked as PAID!`);
      } else if (type === 'DEPOSIT_TOPUP' && therapistId && amount > 0) {
        // 2. Add deposit balance to Therapist
        const { data: therapist } = await supabase
          .from('therapists')
          .select('deposit_balance, name')
          .eq('id', therapistId)
          .single();

        if (therapist) {
          const newDeposit = (Number(therapist.deposit_balance) || 0) + amount;
          await supabase
            .from('therapists')
            .update({ deposit_balance: newDeposit })
            .eq('id', therapistId);

          console.log(`[DOKU Webhook] Added Rp ${amount} to Therapist ${therapist.name} (New Deposit: ${newDeposit})`);
        }
      }
    }

    // Return official DOKU ACK response
    return NextResponse.json(
      {
        response: {
          status: 'SUCCESS',
          message: 'Payment notification processed successfully',
          invoice_number: invoiceNumber,
        },
      },
      {
        status: 200,
        headers: {
          'Response-Timestamp': new Date().toISOString(),
        },
      }
    );
  } catch (error: any) {
    console.error('[DOKU Webhook] Processing error:', error);
    return NextResponse.json(
      { error: error?.message || 'Failed to process DOKU notification' },
      { status: 500 }
    );
  }
}
