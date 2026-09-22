package com.jjjbazar.smsgateway;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Crash-safe main screen.
 * Second open must not force-close.
 */
public class MainActivity extends Activity {
    private static final int SMS_REQ = 501;
    private static final int NOTIF_REQ = 502;

    private TextView statusCard, onlineDot, onlineLabel;
    private Button onlineBtn, permissionBtn, batteryBtn, scanBtn;
    private Button btnBkash, btnNagad, btnRocket;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            try {
                refreshUI();
            } catch (Exception ignored) {
            }
            uiHandler.postDelayed(this, 10_000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
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

            if (onlineBtn != null) onlineBtn.setOnClickListener(v -> safeToggleOnline());
            if (permissionBtn != null) permissionBtn.setOnClickListener(v -> askAllPermissions());
            if (batteryBtn != null) batteryBtn.setOnClickListener(v -> openBatterySettings());
            if (scanBtn != null) scanBtn.setOnClickListener(v -> scanRecentSms());

            if (btnBkash != null) {
                btnBkash.setOnClickListener(v -> {
                    AppPrefs.setBkashEnabled(this, !AppPrefs.isBkashEnabled(this));
                    refreshUI();
                });
            }
            if (btnNagad != null) {
                btnNagad.setOnClickListener(v -> {
                    AppPrefs.setNagadEnabled(this, !AppPrefs.isNagadEnabled(this));
                    refreshUI();
                });
            }
            if (btnRocket != null) {
                btnRocket.setOnClickListener(v -> {
                    AppPrefs.setRocketEnabled(this, !AppPrefs.isRocketEnabled(this));
                    refreshUI();
                });
            }

            // Ask SMS only if missing — do not auto-jump to other settings on every open
            if (!hasSmsPermission()) {
                askAllPermissions();
            }

            refreshUI();

            // Delay scheduler so UI is stable first (prevents relaunch crash)
            uiHandler.postDelayed(() -> {
                try {
                    if (AppPrefs.isOnline(this) && hasSmsPermission()) {
                        ScanScheduler.start(this);
                    }
                } catch (Exception e) {
                    // never crash activity
                }
            }, 1500);

        } catch (Exception e) {
            // Last resort — show empty activity instead of force close
            try {
                Toast.makeText(this, "Open error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // If last scan is older than 2.5 min while Online → force scan + restart scheduler
            if (AppPrefs.isOnline(this) && hasSmsPermission()) {
                long last = AppPrefs.getLastScanTime(this);
                long age = System.currentTimeMillis() - last;
                if (last <= 0 || age > 150_000) {
                    uiHandler.post(() -> {
                        try {
                            ScanScheduler.start(this);
                            new Thread(() -> {
                                try {
                                    SilentScanner.scan(MainActivity.this);
                                } catch (Exception ignored) {
                                }
                                runOnUiThread(() -> {
                                    try { refreshUI(); } catch (Exception ignored) {}
                                });
                            }).start();
                        } catch (Exception ignored) {
                        }
                    });
                } else {
                    // Keep service alive
                    try { AutoScanService.start(this); } catch (Exception ignored) {}
                }
            }
            refreshUI();
            uiHandler.removeCallbacks(refreshTask);
            uiHandler.postDelayed(refreshTask, 8_000);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(refreshTask);
    }

    private void safeToggleOnline() {
        try {
            boolean now = !AppPrefs.isOnline(this);
            AppPrefs.setOnline(this, now);
            if (now) {
                ScanScheduler.start(this);
                Toast.makeText(this, "Online — Auto scan every 2 min", Toast.LENGTH_SHORT).show();
            } else {
                ScanScheduler.cancel(this);
                Toast.makeText(this, "Offline — Auto scan stopped", Toast.LENGTH_SHORT).show();
            }
            refreshUI();
        } catch (Exception e) {
            Toast.makeText(this, "Toggle failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void styleMethodBtn(Button btn, boolean on, String name) {
        if (btn == null) return;
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
        if (statusCard == null) return;

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

        if (onlineDot != null && onlineLabel != null && onlineBtn != null) {
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
        }

        StringBuilder sb = new StringBuilder();
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(online ? "🟢  GATEWAY ONLINE\n" : "🔴  GATEWAY OFFLINE\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("SMS Permission :  ").append(smsOk ? "✅ Allowed" : "❌ Not Allowed").append("\n");
        sb.append("Battery        :  ").append(batteryOk ? "✅ Unrestricted" : "⚠️ Restricted").append("\n");
        sb.append("Auto Scan      :  ").append(online && smsOk ? "✅ Every 2 min" : "⏸ Stopped").append("\n");
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
            sb.append("সব ঠিক।\nপ্রতি ২ মিনিটে স্ক্যান চলবে।");
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
        try {
            return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBatteryUnrestricted() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null) return pm.isIgnoringBatteryOptimizations(getPackageName());
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private void askAllPermissions() {
        try {
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
                    return;
                }
            }
            if (AppPrefs.isOnline(this)) {
                ScanScheduler.start(this);
            }
            refreshUI();
        } catch (Exception e) {
            Toast.makeText(this, "Permission error", Toast.LENGTH_SHORT).show();
        }
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
        try {
            refreshUI();
            if (requestCode == SMS_REQ) {
                boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
                Toast.makeText(this, granted ? "SMS Permission ✅" : "SMS Permission ❌", Toast.LENGTH_SHORT).show();
                if (granted && AppPrefs.isOnline(this)) {
                    uiHandler.postDelayed(() -> {
                        try {
                            ScanScheduler.start(this);
                        } catch (Exception ignored) {
                        }
                    }, 500);
                }
            }
            if (requestCode == NOTIF_REQ && AppPrefs.isOnline(this) && hasSmsPermission()) {
                uiHandler.postDelayed(() -> {
                    try {
                        ScanScheduler.start(this);
                    } catch (Exception ignored) {
                    }
                }, 500);
            }
        } catch (Exception ignored) {
        }
    }

    private void scanRecentSms() {
        try {
            if (!hasSmsPermission()) {
                askAllPermissions();
                return;
            }
            if (!AppPrefs.isOnline(this)) {
                Toast.makeText(this, "আগে Online করুন", Toast.LENGTH_SHORT).show();
                return;
            }
            if (statusCard != null) statusCard.setText("Manual scanning...");
            new Thread(() -> {
                int found = 0;
                try {
                    found = SilentScanner.scan(this);
                } catch (Exception ignored) {
                }
                final int total = found;
                runOnUiThread(() -> {
                    Toast.makeText(this, total + " Receive SMS uploaded", Toast.LENGTH_SHORT).show();
                    refreshUI();
                });
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Scan error", Toast.LENGTH_SHORT).show();
        }
    }
}
