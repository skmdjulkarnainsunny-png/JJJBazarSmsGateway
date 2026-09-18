package com.jjjbazar.smsgateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * Helper service. Main scheduling is AlarmManager (ScanScheduler).
 * Runs as foreground so system is less likely to kill it when minimized.
 */
public class AutoScanService extends Service {
    private static final String TAG = "SaadAutoScan";
    private static final String CH = "saad_gateway_fg";
    private static final int NOTIF_ID = 1001;

    private HandlerThread workerThread;
    private Handler workerHandler;
    private boolean running = false;

    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            try {
                if (AppPrefs.isOnline(getApplicationContext())) {
                    SilentScanner.scan(getApplicationContext());
                }
            } catch (Exception e) {
                Log.e(TAG, "scan error", e);
            }
            if (running && workerHandler != null) {
                workerHandler.postDelayed(this, ScanScheduler.INTERVAL_MS);
            }
        }
    };

    public static void start(Context context) {
        try {
            Intent i = new Intent(context, AutoScanService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Exception e) {
            Log.e(TAG, "start failed", e);
        }
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, AutoScanService.class));
        } catch (Exception e) {
            Log.e(TAG, "stop failed", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        workerThread = new HandlerThread("SaadAutoScanWorker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());
        startAsForeground();
        Log.d(TAG, "Service created (foreground)");
    }

    private void startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH, "Gateway Status", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Shows when auto SMS scan is running");
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CH);
        } else {
            b = new Notification.Builder(this);
        }
        b.setContentTitle("JJJ Gateway Online")
                .setContentText("Auto scan every 3 min")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN);

        startForeground(NOTIF_ID, b.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();
        if (!running) {
            running = true;
            workerHandler.removeCallbacks(scanTask);
            workerHandler.postDelayed(scanTask, 20_000);
            Log.d(TAG, "Loop started");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (workerHandler != null) workerHandler.removeCallbacks(scanTask);
        if (workerThread != null) workerThread.quitSafely();
        Log.d(TAG, "Service destroyed");
        // If still online, reschedule alarm so scan continues after kill
        if (AppPrefs.isOnline(this)) {
            ScanScheduler.scheduleNext(this);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
