package com.jjjbazar.smsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "SaadBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {

            Log.d(TAG, "Boot completed");
            if (AppPrefs.isOnline(context)) {
                ScanScheduler.start(context);
                Log.d(TAG, "ScanScheduler started after boot");
            }
        }
    }
}
