package com.jjjbazar.smsgateway;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

public class NetworkUtil {
    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network n = cm.getActiveNetwork();
                if (n == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(n);
                if (caps == null) return false;
                return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
            }
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}
