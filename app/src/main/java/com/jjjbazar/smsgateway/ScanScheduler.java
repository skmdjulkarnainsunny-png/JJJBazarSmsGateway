package com.jjjbazar.smsgateway;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Reliable 2-minute schedule using setAlarmClock (bypasses Doze).
 * Also keeps AutoScanService alive.
 */
public class ScanScheduler {
    private static final String TAG = "JjjScanScheduler";
    public static final long INTERVAL_MS = 2 * 60 * 1000L; // 2 minutes
    private static final int REQ_CODE = 4401;

    public static void start(Context context) {
        Context app = context.getApplicationContext();
        try {
            cancelAlarmsOnly(app);
            if (!AppPrefs.isOnline(app)) {
                Log.d(TAG, "Offline — not scheduling");
                return;
            }
            // First fire soon, then every 2 min via receiver chain
            scheduleAt(app, System.currentTimeMillis() + 5_000);
            try {
                AutoScanService.start(app);
            } catch (Exception e) {
                Log.e(TAG, "Service start failed", e);
            }
            Log.d(TAG, "Scheduler started (2 min, AlarmClock)");
        } catch (Exception e) {
            Log.e(TAG, "start failed", e);
        }
    }

    public static void scheduleNext(Context context) {
        Context app = context.getApplicationContext();
        try {
            if (!AppPrefs.isOnline(app)) return;
            scheduleAt(app, System.currentTimeMillis() + INTERVAL_MS);
        } catch (Exception e) {
            Log.e(TAG, "scheduleNext failed", e);
        }
    }

    public static void cancel(Context context) {
        Context app = context.getApplicationContext();
        try {
            cancelAlarmsOnly(app);
            AutoScanService.stop(app);
            Log.d(TAG, "Scheduler cancelled");
        } catch (Exception e) {
            Log.e(TAG, "cancel failed", e);
        }
    }

    private static void cancelAlarmsOnly(Context app) {
        try {
            AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(pending(app));
        } catch (Exception ignored) {
        }
    }

    private static void scheduleAt(Context app, long triggerAt) {
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(app);

        // setAlarmClock is most reliable under Doze (may show small alarm icon)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlarmManager.AlarmClockInfo info =
                        new AlarmManager.AlarmClockInfo(triggerAt, pi);
                am.setAlarmClock(info, pi);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "setAlarmClock failed, fallback", e);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (Exception e) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } catch (Exception ignored) {
            }
        }
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
