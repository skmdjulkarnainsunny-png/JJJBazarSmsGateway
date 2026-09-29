package com.jjjbazar.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Manifest receiver — অ্যাপ বন্ধ থাকলেও নতুন SMS পায়।
 * Force Stop করলে সিস্টেম আর SMS পাঠায় না।
 */
public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "JjjSmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();

        // Extract SMS on main thread quickly (intent extras can be recycled)
        final SmsMessage[] messages;
        try {
            messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        } catch (Exception e) {
            Log.e(TAG, "getMessages failed", e);
            pending.finish();
            return;
        }

        new Thread(() -> {
            PowerManager.WakeLock wl = null;
            try {
                PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JjjGateway:SmsRecv");
                    wl.setReferenceCounted(false);
                    wl.acquire(90_000);
                }

                if (!AppPrefs.isOnline(app)) {
                    Log.d(TAG, "OFFLINE — ignore SMS");
                    return;
                }

                // Keep FGS + alarm alive in background
                try {
                    AutoScanService.start(app);
                    ScanScheduler.scheduleNext(app);
                } catch (Exception ignored) {
                }

                if (messages == null || messages.length == 0) return;

                String sender = messages[0].getOriginatingAddress();
                long timestamp = messages[0].getTimestampMillis();
                StringBuilder body = new StringBuilder();
                for (SmsMessage msg : messages) {
                    if (msg != null && msg.getMessageBody() != null) {
                        body.append(msg.getMessageBody());
                    }
                }

                String fullBody = body.toString();
                Log.d(TAG, "SMS from=" + sender);

                PaymentSms p = SmsParser.parse(sender, fullBody, timestamp);
                if (p == null) {
                    Log.d(TAG, "Not receive/cash-in — skip");
                    return;
                }
                if (!AppPrefs.isMethodEnabled(app, p.method)) {
                    Log.d(TAG, "Method OFF: " + p.method);
                    return;
                }

                // Blocking-style upload wait via simple sleep after fire
                final Object lock = new Object();
                final boolean[] done = {false};
                Uploader.upload(app, p, (ok, err) -> {
                    Log.d(TAG, "Upload " + (ok ? "OK" : "FAIL") + " tx=" + p.txid + " " + err);
                    AppPrefs.setLastScan(app, System.currentTimeMillis(), ok ? 1 : 0);
                    synchronized (lock) {
                        done[0] = true;
                        lock.notifyAll();
                    }
                });

                // Wait up to 25s for Firestore callback
                synchronized (lock) {
                    long end = System.currentTimeMillis() + 25_000;
                    while (!done[0] && System.currentTimeMillis() < end) {
                        try {
                            lock.wait(1000);
                        } catch (InterruptedException ignored) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "SMS process error", e);
            } finally {
                try {
                    if (wl != null && wl.isHeld()) wl.release();
                } catch (Exception ignored) {
                }
                pending.finish();
            }
        }, "JjjSmsWorker").start();
    }
}
