package com.jjjbazar.smsgateway;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Telephony;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class SilentScanner {
    private static final String TAG = "SaadSilentScanner";
    private static final int SCAN_LIMIT = 50;

    public static int scan(Context context) {
        if (!AppPrefs.isOnline(context)) {
            Log.d(TAG, "Offline — skip scan");
            return 0;
        }

        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No READ_SMS permission");
            AppPrefs.setLastScan(context, System.currentTimeMillis(), 0);
            return 0;
        }

        int found = 0;
        Set<String> seen = new HashSet<>();
        Cursor c = null;

        try {
            ContentResolver cr = context.getContentResolver();
            c = cr.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    new String[]{
                            Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE
                    },
                    null, null,
                    Telephony.Sms.DEFAULT_SORT_ORDER
            );

            int limit = SCAN_LIMIT;
            while (c != null && c.moveToNext() && limit-- > 0) {
                String sender = c.getString(0);
                String body = c.getString(1);
                long date = c.getLong(2);

                PaymentSms p = SmsParser.parse(sender, body, date);
                if (p == null) continue;

                // Per-method toggle
                if (!AppPrefs.isMethodEnabled(context, p.method)) {
                    continue;
                }

                if (seen.add(p.txid)) {
                    found++;
                    Uploader.upload(context, p, null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Silent scan failed", e);
        } finally {
            if (c != null) c.close();
        }

        AppPrefs.setLastScan(context, System.currentTimeMillis(), found);
        Log.d(TAG, "Silent scan done. Found: " + found);
        return found;
    }
}
