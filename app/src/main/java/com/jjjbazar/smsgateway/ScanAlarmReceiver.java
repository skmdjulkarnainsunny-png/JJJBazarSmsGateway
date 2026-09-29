package com.jjjbazar.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Handles both 2-min scan and 10-min watchdog.
 * Always reschedules while gateway Online — never stops unless user turns Offline.
 */
public class ScanAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "JjjScanAlarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        final String action = intent != null ? intent.getAction() : "";
        final boolean isWatch = "com.jjjbazar.smsgateway.ACTION_WATCHDOG".equals(action);

        new Thread(() -> {
            PowerManager.WakeLock wl = null;
            try {
                PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JjjGateway:Alarm");
                    wl.setReferenceCounted(false);
                    wl.acquire(90_000);
                }

                if (!AppPrefs.isOnline(app)) {
                    Log.d(TAG, "Gateway Offline — stop");
                    return;
                }

                // Always revive service
                try {
                    AutoScanService.start(app);
                } catch (Exception ignored) {
                }

                if (isWatch) {
                    Log.d(TAG, "Watchdog tick — service revived");
                    AppPrefs.setStatusMsg(app, "Watchdog OK");
                    // also do a scan on watchdog
                }

                boolean net = NetworkUtil.isOnline(app);
                if (!net) {
                    AppPrefs.setNetOk(app, false);
                    AppPrefs.setLastScan(app, System.currentTimeMillis(), 0);
                    AppPrefs.setStatusMsg(app, "No internet — retry 10 min");
                    ScanScheduler.scheduleNetRetry(app);
                    ScanScheduler.scheduleWatchdog(app);
                    return;
                }

                AppPrefs.setNetOk(app, true);
                int found = 0;
                try {
                    found = SilentScanner.scan(app);
                } catch (Exception e) {
                    Log.e(TAG, "scan error", e);
                }
                AppPrefs.setStatusMsg(app, "Scan OK · found " + found);
                ScanScheduler.scheduleNextScan(app);
                ScanScheduler.scheduleWatchdog(app);
                Log.d(TAG, "Scan done found=" + found);
            } catch (Exception e) {
                Log.e(TAG, "alarm failed", e);
                try {
                    if (AppPrefs.isOnline(app)) {
                        ScanScheduler.scheduleNextScan(app);
                        ScanScheduler.scheduleWatchdog(app);
                    }
                } catch (Exception ignored) {
                }
            } finally {
                try {
                    if (wl != null && wl.isHeld()) wl.release();
                } catch (Exception ignored) {
                }
                pending.finish();
            }
        }, "JjjAlarmWorker").start();
    }
}
