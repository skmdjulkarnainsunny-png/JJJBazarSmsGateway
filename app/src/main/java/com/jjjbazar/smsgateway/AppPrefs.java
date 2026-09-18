package com.jjjbazar.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppPrefs {
    private static final String PREF = "saad_gateway_prefs";
    private static final String KEY_ONLINE = "is_online";
    private static final String KEY_LAST_SCAN = "last_auto_scan";
    private static final String KEY_LAST_FOUND = "last_auto_found";
    private static final String KEY_BKASH = "enable_bkash";
    private static final String KEY_NAGAD = "enable_nagad";
    private static final String KEY_ROCKET = "enable_rocket";

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
}
