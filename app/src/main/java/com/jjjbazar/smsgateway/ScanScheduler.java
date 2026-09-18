package com.jjjbazar.smsgateway;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * AlarmManager দিয়ে প্রতি ৩ মিনিটে স্ক্যান শিডিউল।
 * অ্যাপ recent থেকে কিল হলেও অ্যালার্ম থেকে আবার চালু হয়।
 */
public class ScanScheduler {
    private static final String TAG = "SaadScanScheduler";
    public static final long INTERVAL_MS = 3 * 60 * 1000L;
    private static final int REQ_CODE = 4401;

    public static void start(Context context) {
        Context app = context.getApplicationContext();
        cancel(app);

        if (!AppPrefs.isOnline(app)) {
            Log.d(TAG, "Offline — not scheduling");
            return;
        }

        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = pending(app);
        long trigger = System.currentTimeMillis() + 15_000; // first run after 15s

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
            Log.d(TAG, "Alarm scheduled");
        } catch (Exception e) {
            Log.e(TAG, "schedule failed", e);
            // fallback inexact
            try {
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi);
            } catch (Exception ignored) {
            }
        }

        // Also keep service as helper
        AutoScanService.start(app);
    }

    public static void scheduleNext(Context context) {
        Context app = context.getApplicationContext();
        if (!AppPrefs.isOnline(app)) return;

        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = pending(app);
        long trigger = System.currentTimeMillis() + INTERVAL_MS;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        } catch (Exception e) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi);
            } catch (Exception ignored) {
            }
        }
    }

    public static void cancel(Context context) {
        Context app = context.getApplicationContext();
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pending(app));
        }
        AutoScanService.stop(app);
        Log.d(TAG, "Alarm cancelled");
    }

    private static PendingIntent pending(Context context) {
        Intent i = new Intent(context, ScanAlarmReceiver.class);
        i.setAction("com.jjjbazar.smsgateway.ACTION_AUTO_SCAN");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, REQ_CODE, i, flags);
    }
}
