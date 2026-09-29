package com.jjjbazar.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppPrefs {
    private static final String PREF = "jjj_gateway_prefs";
    private static final String KEY_ONLINE = "gateway_online";
    private static final String KEY_LAST_SCAN = "last_auto_scan";
    private static final String KEY_LAST_FOUND = "last_auto_found";
    private static final String KEY_BKASH = "enable_bkash";
    private static final String KEY_NAGAD = "enable_nagad";
    private static final String KEY_ROCKET = "enable_rocket";
    private static final String KEY_NET_OK = "last_net_ok";
    private static final String KEY_LAST_MSG = "last_status_msg";
    private static final String KEY_SCAN_SEC = "scan_interval_sec";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean isOnline(Context context) {
        return p(context).getBoolean(KEY_ONLINE, true);
    }

    public static void setOnline(Context context, boolean online) {
        p(context).edit().putBoolean(KEY_ONLINE, online).apply();
    }

    public static boolean isBkashEnabled(Context context) {
        return p(context).getBoolean(KEY_BKASH, true);
    }

    public static boolean isNagadEnabled(Context context) {
        return p(context).getBoolean(KEY_NAGAD, true);
    }

    public static boolean isRocketEnabled(Context context) {
        return p(context).getBoolean(KEY_ROCKET, true);
    }

    public static void setBkashEnabled(Context context, boolean v) {
        p(context).edit().putBoolean(KEY_BKASH, v).apply();
    }

    public static void setNagadEnabled(Context context, boolean v) {
        p(context).edit().putBoolean(KEY_NAGAD, v).apply();
    }

    public static void setRocketEnabled(Context context, boolean v) {
        p(context).edit().putBoolean(KEY_ROCKET, v).apply();
    }

    public static boolean isMethodEnabled(Context context, String method) {
        if (method == null) return false;
        switch (method) {
            case "bKash": return isBkashEnabled(context);
            case "Nagad": return isNagadEnabled(context);
            case "Rocket": return isRocketEnabled(context);
            default: return false;
        }
    }

    public static void setLastScan(Context context, long timeMs, int foundCount) {
        p(context).edit()
                .putLong(KEY_LAST_SCAN, timeMs)
                .putInt(KEY_LAST_FOUND, foundCount)
                .apply();
    }

    public static long getLastScanTime(Context context) {
        return p(context).getLong(KEY_LAST_SCAN, 0);
    }

    public static int getLastFound(Context context) {
        return p(context).getInt(KEY_LAST_FOUND, 0);
    }

    public static String getLastScanText(Context context) {
        long t = getLastScanTime(context);
        if (t <= 0) return "Never";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a", Locale.US);
        return sdf.format(new Date(t));
    }

    public static void setNetOk(Context context, boolean ok) {
        p(context).edit().putBoolean(KEY_NET_OK, ok).apply();
    }

    public static boolean wasNetOk(Context context) {
        return p(context).getBoolean(KEY_NET_OK, true);
    }

    public static void setStatusMsg(Context context, String msg) {
        p(context).edit().putString(KEY_LAST_MSG, msg == null ? "" : msg).apply();
    }

    public static String getStatusMsg(Context context) {
        return p(context).getString(KEY_LAST_MSG, "");
    }

    /** Scan interval in seconds (clamped). */
    public static int getScanIntervalSec(Context context) {
        int s = p(context).getInt(KEY_SCAN_SEC, GatewayConfig.DEFAULT_SCAN_SEC);
        if (s < GatewayConfig.MIN_SCAN_SEC) s = GatewayConfig.MIN_SCAN_SEC;
        if (s > GatewayConfig.MAX_SCAN_SEC) s = GatewayConfig.MAX_SCAN_SEC;
        return s;
    }

    public static long getScanIntervalMs(Context context) {
        return getScanIntervalSec(context) * 1000L;
    }

    public static void setScanIntervalSec(Context context, int sec) {
        if (sec < GatewayConfig.MIN_SCAN_SEC) sec = GatewayConfig.MIN_SCAN_SEC;
        if (sec > GatewayConfig.MAX_SCAN_SEC) sec = GatewayConfig.MAX_SCAN_SEC;
        p(context).edit().putInt(KEY_SCAN_SEC, sec).apply();
    }
}
