package com.jjjbazar.smsgateway;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ScanScheduler {
    private static final String TAG = "JjjScanScheduler";
    public static final long NET_RETRY_MS = 10 * 60 * 1000L;
    public static final long WATCHDOG_MS = 10 * 60 * 1000L;
    private static final int REQ_SCAN = 4401;
    private static final int REQ_WATCH = 4402;

    /** @deprecated use AppPrefs.getScanIntervalMs */
    @Deprecated
    public static final long INTERVAL_MS = 120_000L;

    public static void start(Context context) {
        Context app = context.getApplicationContext();
        try {
            if (!AppPrefs.isOnline(app)) {
                cancel(app);
                return;
            }
            scheduleScanAt(app, System.currentTimeMillis() + 5_000);
            scheduleWatchAt(app, System.currentTimeMillis() + WATCHDOG_MS);
            try {
                AutoScanService.start(app);
            } catch (Exception e) {
                Log.e(TAG, "Service start failed", e);
            }
            Log.d(TAG, "START intervalSec=" + AppPrefs.getScanIntervalSec(app));
        } catch (Exception e) {
            Log.e(TAG, "start failed", e);
        }
    }

    public static void scheduleNextScan(Context context) {
        Context app = context.getApplicationContext();
        if (!AppPrefs.isOnline(app)) return;
        long ms = AppPrefs.getScanIntervalMs(app);
        scheduleScanAt(app, System.currentTimeMillis() + ms);
    }

    public static void scheduleNetRetry(Context context) {
        Context app = context.getApplicationContext();
        if (!AppPrefs.isOnline(app)) return;
        scheduleScanAt(app, System.currentTimeMillis() + NET_RETRY_MS);
        AppPrefs.setStatusMsg(app, "No network — retry in 10 min");
    }

    public static void scheduleWatchdog(Context context) {
        Context app = context.getApplicationContext();
        if (!AppPrefs.isOnline(app)) return;
        scheduleWatchAt(app, System.currentTimeMillis() + WATCHDOG_MS);
    }

    public static void cancel(Context context) {
        Context app = context.getApplicationContext();
        try {
            AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(scanPending(app));
                am.cancel(watchPending(app));
            }
            AutoScanService.stop(app);
        } catch (Exception e) {
            Log.e(TAG, "cancel failed", e);
        }
    }

    public static void scheduleNext(Context context) {
        scheduleNextScan(context);
    }

    private static void scheduleScanAt(Context app, long triggerAt) {
        setAlarmClock(app, triggerAt, scanPending(app));
    }

    private static void scheduleWatchAt(Context app, long triggerAt) {
        setAlarmClock(app, triggerAt, watchPending(app));
    }

    private static void setAlarmClock(Context app, long triggerAt, PendingIntent pi) {
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, pi), pi);
                return;
            }
        } catch (Exception ignored) {
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

    private static PendingIntent scanPending(Context context) {
        Intent i = new Intent(context, ScanAlarmReceiver.class);
        i.setAction("com.jjjbazar.smsgateway.ACTION_AUTO_SCAN");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQ_SCAN, i, flags);
    }

    private static PendingIntent watchPending(Context context) {
        Intent i = new Intent(context, ScanAlarmReceiver.class);
        i.setAction("com.jjjbazar.smsgateway.ACTION_WATCHDOG");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, REQ_WATCH, i, flags);
    }
}
