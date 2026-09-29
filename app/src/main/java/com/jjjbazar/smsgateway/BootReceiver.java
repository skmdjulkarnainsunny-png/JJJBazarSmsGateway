package com.jjjbazar.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "JjjBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Log.d(TAG, "action=" + intent.getAction());
        final Context app = context.getApplicationContext();
        if (!AppPrefs.isOnline(app)) return;

        // Delay for network after boot
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                ScanScheduler.start(app);
                AutoScanService.start(app);
                Log.d(TAG, "Restarted after boot/update");
            } catch (Exception e) {
                Log.e(TAG, "boot fail", e);
            }
        }, 12_000);
    }
}
