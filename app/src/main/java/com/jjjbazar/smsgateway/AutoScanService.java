package com.jjjbazar.smsgateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

/**
 * Sticky foreground service. Survives recents clear when possible.
 * Internal 2-min loop + relies on AlarmClock as backup.
 */
public class AutoScanService extends Service {
    private static final String TAG = "JjjAutoScan";
    private static final String CH = "jjj_gateway_fg";
    private static final int NOTIF_ID = 1001;

    private HandlerThread workerThread;
    private Handler workerHandler;
    private boolean running = false;

    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            try {
                if (!AppPrefs.isOnline(getApplicationContext())) {
                    stopSelf();
                    return;
                }
                if (!NetworkUtil.isOnline(getApplicationContext())) {
                    AppPrefs.setNetOk(getApplicationContext(), false);
                    AppPrefs.setStatusMsg(getApplicationContext(), "No internet — retry 10 min");
                    AppPrefs.setLastScan(getApplicationContext(), System.currentTimeMillis(), 0);
                    updateNotification();
                    if (workerHandler != null) {
                        workerHandler.postDelayed(this, ScanScheduler.NET_RETRY_MS);
                    }
                    ScanScheduler.scheduleNetRetry(getApplicationContext());
                    return;
                }
                AppPrefs.setNetOk(getApplicationContext(), true);
                int found = SilentScanner.scan(getApplicationContext());
                AppPrefs.setStatusMsg(getApplicationContext(), "Scan OK · found " + found);
                updateNotification();
                Log.d(TAG, "Loop scan found=" + found);
            } catch (Exception e) {
                Log.e(TAG, "scan error", e);
            }
            if (running && workerHandler != null) {
                workerHandler.postDelayed(this, AppPrefs.getScanIntervalMs(getApplicationContext()));
            }
        }
    };

    public static void start(Context context) {
        try {
            Intent i = new Intent(context, AutoScanService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(i);
                } catch (Exception e) {
                    try {
                        context.startService(i);
                    } catch (Exception e2) {
                        Log.e(TAG, "start failed", e2);
                    }
                }
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
        try {
            workerThread = new HandlerThread("JjjAutoScanWorker");
            workerThread.start();
            workerHandler = new Handler(workerThread.getLooper());
            promoteForegroundSafe();
        } catch (Exception e) {
            Log.e(TAG, "onCreate", e);
        }
    }

    private void promoteForegroundSafe() {
        try {
            Notification n = buildNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (Exception e) {
            Log.e(TAG, "FGS fail", e);
        }
    }

    private void updateNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIF_ID, buildNotification());
        } catch (Exception ignored) {
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH, "Gateway Status", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Keeps auto SMS scan alive");
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, piFlags);

        String last = AppPrefs.getLastScanText(this);
        String msg = AppPrefs.getStatusMsg(this);
        if (msg == null || msg.isEmpty()) msg = "Every 2 min";
        String text = msg + " · Last: " + last;

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(this, CH);
        } else {
            b = new Notification.Builder(this);
        }
        return b.setContentTitle("JJJ Gateway Online")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            promoteForegroundSafe();
            if (!AppPrefs.isOnline(this)) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (!running && workerHandler != null) {
                running = true;
                workerHandler.removeCallbacks(scanTask);
                workerHandler.postDelayed(scanTask, 8_000);
            }
            // Ensure alarms exist even if process was restarted by system
            ScanScheduler.scheduleNextScan(this);
            ScanScheduler.scheduleWatchdog(this);
        } catch (Exception e) {
            Log.e(TAG, "onStartCommand", e);
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // User swiped from recents — do NOT turn offline; reschedule alarms
        Log.d(TAG, "onTaskRemoved — keep Online, reschedule");
        try {
            if (AppPrefs.isOnline(this)) {
                ScanScheduler.scheduleNextScan(this);
                ScanScheduler.scheduleWatchdog(this);
                // restart service shortly via alarm
            }
        } catch (Exception ignored) {
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        running = false;
        try {
            if (workerHandler != null) workerHandler.removeCallbacks(scanTask);
            if (workerThread != null) workerThread.quitSafely();
        } catch (Exception ignored) {
        }
        try {
            if (AppPrefs.isOnline(this)) {
                ScanScheduler.scheduleNextScan(this);
                ScanScheduler.scheduleWatchdog(this);
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
