package com.jjjbazar.smsgateway;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Auto SMS receiver.
 * শুধু Online থাকলে + Money Received / Cash In SMS আপলোড করবে।
 */
public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SaadSmsReceiver";
    private static final String CHANNEL_ID = "saad_sms_gateway";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        final PendingResult pendingResult = goAsync();
        final Context appContext = context.getApplicationContext();

        try {
            // Offline থাকলে কোনো SMS প্রসেস করবে না
            if (!AppPrefs.isOnline(appContext)) {
                Log.d(TAG, "App is OFFLINE — ignoring SMS");
                pendingResult.finish();
                return;
            }

            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (messages == null || messages.length == 0) {
                pendingResult.finish();
                return;
            }

            String sender = messages[0].getOriginatingAddress();
            long timestamp = messages[0].getTimestampMillis();
            StringBuilder body = new StringBuilder();

            for (SmsMessage msg : messages) {
                if (msg != null && msg.getMessageBody() != null) {
                    body.append(msg.getMessageBody());
                }
            }

            String fullBody = body.toString();
            Log.d(TAG, "SMS from: " + sender);

            PaymentSms p = SmsParser.parse(sender, fullBody, timestamp);
            if (p == null) {
                Log.d(TAG, "Ignored (not Money Received / Cash In)");
                pendingResult.finish();
                return;
            }

            if (!AppPrefs.isMethodEnabled(appContext, p.method)) {
                Log.d(TAG, "Method disabled: " + p.method);
                pendingResult.finish();
                return;
            }

            Log.d(TAG, "Receive SMS → " + p.method + " ৳" + p.amount + " | " + p.txid);

            Uploader.upload(appContext, p, (ok, message) -> {
                Log.d(TAG, "Upload: " + ok + " → " + message);
                if (ok) {
                    showNotification(appContext, p);
                }
                pendingResult.finish();
            });

        } catch (Exception e) {
            Log.e(TAG, "Receiver failed", e);
            pendingResult.finish();
        }
    }

    private void showNotification(Context context, PaymentSms p) {
        try {
            createChannel(context);

            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                    context, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("✅ " + p.method + " Received")
                    .setContentText("৳" + p.amount + "  •  " + p.txid)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(p.method + " Cash In / Money Received\nAmount: ৳" + p.amount + "\nTxnID: " + p.txid))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                if (Build.VERSION.SDK_INT >= 33) {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Notification failed", e);
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Gateway",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Payment receive notifications");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
