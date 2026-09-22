package com.jjjbazar.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "JjjBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "Boot action=" + action);
        try {
            Context app = context.getApplicationContext();
            if (AppPrefs.isOnline(app)) {
                // Delay slightly after boot so network is up
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        ScanScheduler.start(app);
                        AutoScanService.start(app);
                        Log.d(TAG, "Scheduler started after boot");
                    } catch (Exception e) {
                        Log.e(TAG, "boot start fail", e);
                    }
                }, 15_000);
            }
        } catch (Exception e) {
            Log.e(TAG, "onReceive", e);
        }
    }
}
