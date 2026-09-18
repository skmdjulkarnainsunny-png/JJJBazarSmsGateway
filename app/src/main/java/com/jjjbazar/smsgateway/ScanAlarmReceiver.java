package com.jjjbazar.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * AlarmManager থেকে প্রতি ৩ মিনিটে কল হয়।
 * অ্যাপ কিল থাকলেও এই receiver চলে।
 */
public class ScanAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "SaadScanAlarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();

        new Thread(() -> {
            PowerManager.WakeLock wl = null;
            try {
                PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SaadGateway:AlarmScan");
                    wl.setReferenceCounted(false);
                    wl.acquire(45_000);
                }

                if (AppPrefs.isOnline(app)) {
                    int found = SilentScanner.scan(app);
                    Log.d(TAG, "Alarm scan done, found=" + found);
                } else {
                    Log.d(TAG, "Offline — skip");
                }
            } catch (Exception e) {
                Log.e(TAG, "Alarm scan failed", e);
            } finally {
                try {
                    if (wl != null && wl.isHeld()) wl.release();
                } catch (Exception ignored) {
                }
                // Next alarm
                if (AppPrefs.isOnline(app)) {
                    ScanScheduler.scheduleNext(app);
                }
                pending.finish();
            }
        }).start();
    }
}
