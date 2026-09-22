package com.jjjbazar.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Fires every ~2 min. Scans + reschedules + keeps service alive.
 */
public class ScanAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "JjjScanAlarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();

        new Thread(() -> {
            PowerManager.WakeLock wl = null;
            try {
                PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "JjjGateway:AlarmScan");
                    wl.setReferenceCounted(false);
                    wl.acquire(60_000);
                }

                if (AppPrefs.isOnline(app)) {
                    // Ensure service is running for in-process 2-min loop
                    try {
                        AutoScanService.start(app);
                    } catch (Exception ignored) {
                    }

                    int found = SilentScanner.scan(app);
                    Log.d(TAG, "Alarm scan done, found=" + found
                            + " last=" + AppPrefs.getLastScanText(app));
                } else {
                    Log.d(TAG, "Offline — skip");
                }
            } catch (Exception e) {
                Log.e(TAG, "Alarm scan failed", e);
                // Still stamp last attempt so UI shows activity
                try {
                    AppPrefs.setLastScan(app, System.currentTimeMillis(), 0);
                } catch (Exception ignored) {
                }
            } finally {
                try {
                    if (wl != null && wl.isHeld()) wl.release();
                } catch (Exception ignored) {
                }
                try {
                    if (AppPrefs.isOnline(app)) {
                        ScanScheduler.scheduleNext(app);
                    }
                } catch (Exception ignored) {
                }
                pending.finish();
            }
        }).start();
    }
}
