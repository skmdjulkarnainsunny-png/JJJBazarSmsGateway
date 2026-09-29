package com.jjjbazar.smsgateway;

import android.app.Application;
import android.util.Log;

public class JjjApp extends Application {
    private static final String TAG = "JjjApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (AppPrefs.isOnline(this)) {
                ScanScheduler.start(this);
                Log.d(TAG, "Process start → scheduler kept alive");
            }
        } catch (Exception e) {
            Log.e(TAG, "onCreate", e);
        }
    }
}
