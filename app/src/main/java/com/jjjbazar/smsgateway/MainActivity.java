package com.jjjbazar.smsgateway;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int SMS_REQ = 501;
    private static final int NOTIF_REQ = 502;

    private TextView statusCard, onlineDot, onlineLabel;
    private Button onlineBtn, permissionBtn, batteryBtn, scanBtn;
    private Button btnBkash, btnNagad, btnRocket;

    private final Handler uiHandler = new Handler();
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            refreshUI();
            uiHandler.postDelayed(this, 10_000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        statusCard = findViewById(R.id.statusCard);
        onlineDot = findViewById(R.id.onlineDot);
        onlineLabel = findViewById(R.id.onlineLabel);
        onlineBtn = findViewById(R.id.onlineBtn);
        permissionBtn = findViewById(R.id.permissionBtn);
        batteryBtn = findViewById(R.id.batteryBtn);
        scanBtn = findViewById(R.id.scanBtn);
        btnBkash = findViewById(R.id.btnBkash);
        btnNagad = findViewById(R.id.btnNagad);
        btnRocket = findViewById(R.id.btnRocket);

        onlineBtn.setOnClickListener(v -> toggleOnline());
        permissionBtn.setOnClickListener(v -> askAllPermissions());
        batteryBtn.setOnClickListener(v -> openBatterySettings());
        scanBtn.setOnClickListener(v -> scanRecentSms());

        btnBkash.setOnClickListener(v -> {
            AppPrefs.setBkashEnabled(this, !AppPrefs.isBkashEnabled(this));
            refreshUI();
        });
        btnNagad.setOnClickListener(v -> {
            AppPrefs.setNagadEnabled(this, !AppPrefs.isNagadEnabled(this));
            refreshUI();
        });
        btnRocket.setOnClickListener(v -> {
            AppPrefs.setRocketEnabled(this, !AppPrefs.isRocketEnabled(this));
            refreshUI();
        });

        askAllPermissions();
        requestExactAlarmIfNeeded();
        refreshUI();

        if (AppPrefs.isOnline(this) && hasSmsPermission()) {
            ScanScheduler.start(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
        uiHandler.removeCallbacks(refreshTask);
        uiHandler.postDelayed(refreshTask, 10_000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(refreshTask);
    }

    private void toggleOnline() {
        boolean now = !AppPrefs.isOnline(this);
        AppPrefs.setOnline(this, now);
        if (now) {
            ScanScheduler.start(this);
            Toast.makeText(this, "Online — Auto scan every 3 min", Toast.LENGTH_SHORT).show();
        } else {
            ScanScheduler.cancel(this);
            Toast.makeText(this, "Offline — Auto scan stopped", Toast.LENGTH_SHORT).show();
        }
        refreshUI();
    }

    private void styleMethodBtn(Button btn, boolean on, String name) {
        if (on) {
            btn.setText(name + " ON");
            btn.setBackgroundColor(Color.parseColor("#166534"));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setText(name + " OFF");
            btn.setBackgroundColor(Color.parseColor("#374151"));
            btn.setTextColor(Color.parseColor("#9CA3AF"));
        }
    }

    private void refreshUI() {
        boolean online = AppPrefs.isOnline(this);
        boolean smsOk = hasSmsPermission();
        boolean batteryOk = isBatteryUnrestricted();
        String lastScan = AppPrefs.getLastScanText(this);
        int lastFound = AppPrefs.getLastFound(this);
        boolean bk = AppPrefs.isBkashEnabled(this);
        boolean ng = AppPrefs.isNagadEnabled(this);
        boolean rk = AppPrefs.isRocketEnabled(this);

        styleMethodBtn(btnBkash, bk, "bKash");
        styleMethodBtn(btnNagad, ng, "Nagad");
        styleMethodBtn(btnRocket, rk, "Rocket");

        if (online) {
            onlineDot.setBackgroundColor(Color.parseColor("#22C55E"));
            onlineLabel.setText("ONLINE");
            onlineLabel.setTextColor(Color.parseColor("#16A34A"));
            onlineBtn.setText("Go Offline");
            onlineBtn.setBackgroundColor(Color.parseColor("#FEE2E2"));
            onlineBtn.setTextColor(Color.parseColor("#B91C1C"));
        } else {
            onlineDot.setBackgroundColor(Color.parseColor("#EF4444"));
            onlineLabel.setText("OFFLINE");
            onlineLabel.setTextColor(Color.parseColor("#DC2626"));
            onlineBtn.setText("Go Online");
            onlineBtn.setBackgroundColor(Color.parseColor("#DCFCE7"));
            onlineBtn.setTextColor(Color.parseColor("#15803D"));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(online ? "🟢  GATEWAY ONLINE\n" : "🔴  GATEWAY OFFLINE\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("SMS Permission :  ").append(smsOk ? "✅ Allowed" : "❌ Not Allowed").append("\n");
        sb.append("Battery        :  ").append(batteryOk ? "✅ Unrestricted" : "⚠️ Restricted").append("\n");
        sb.append("Auto Scan      :  ").append(online && smsOk ? "✅ Every 3 min" : "⏸ Stopped").append("\n");
        sb.append("Methods        :  ");
        if (bk) sb.append("bKash ");
        if (ng) sb.append("Nagad ");
        if (rk) sb.append("Rocket ");
        if (!bk && !ng && !rk) sb.append("None");
        sb.append("\n\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("Last auto scan :  ").append(lastScan).append("\n");
        if (AppPrefs.getLastScanTime(this) > 0) {
            sb.append("Found last run :  ").append(lastFound).append(" SMS\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");

        if (online && smsOk && batteryOk) {
            sb.append("সব ঠিক।\nপ্রতি ৩ মিনিটে স্ক্যান চলবে\n(অ্যাপ মিনিমাইজ/কিল হলেও)।");
            statusCard.setBackgroundColor(Color.parseColor("#ECFDF5"));
        } else if (!online) {
            sb.append("Offline। Auto scan বন্ধ।");
            statusCard.setBackgroundColor(Color.parseColor("#FEF2F2"));
        } else {
            sb.append("Permission / Battery ঠিক করুন।");
            statusCard.setBackgroundColor(Color.parseColor("#FFFBEB"));
        }
        statusCard.setText(sb.toString());
    }

    private boolean hasSmsPermission() {
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isBatteryUnrestricted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void requestExactAlarmIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (am != null && !am.canScheduleExactAlarms()) {
                    Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void askAllPermissions() {
        if (!hasSmsPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
            }, SMS_REQ);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIF_REQ);
            }
        }
        if (AppPrefs.isOnline(this)) {
            ScanScheduler.start(this);
        }
        refreshUI();
    }

    private void openBatterySettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "Allow → Unrestricted চাপুন", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Settings error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshUI();
        if (requestCode == SMS_REQ) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this, granted ? "SMS Permission ✅" : "SMS Permission ❌", Toast.LENGTH_SHORT).show();
            if (granted && AppPrefs.isOnline(this)) {
                ScanScheduler.start(this);
            }
        }
    }

    private void scanRecentSms() {
        if (!hasSmsPermission()) {
            askAllPermissions();
            return;
        }
        if (!AppPrefs.isOnline(this)) {
            Toast.makeText(this, "আগে Online করুন", Toast.LENGTH_SHORT).show();
            return;
        }
        statusCard.setText("Manual scanning...");
        new Thread(() -> {
            int found = SilentScanner.scan(this);
            runOnUiThread(() -> {
                Toast.makeText(this, found + " Receive SMS uploaded", Toast.LENGTH_SHORT).show();
                refreshUI();
            });
        }).start();
    }
}
