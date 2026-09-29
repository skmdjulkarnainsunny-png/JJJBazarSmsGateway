package com.jjjbazar.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Upload only if trxId not in DB. Never overwrite approved.
 * source = GatewayConfig.SOURCE (hard token).
 */
public class Uploader {
    private static final String TAG = "JjjUploader";
    private static final String PREF = "jjj_uploaded_txids";
    private static final String KEY_IDS = "ids";

    public interface Callback {
        void done(boolean ok, String message);
    }

    public static void upload(Context context, PaymentSms p, Callback callback) {
        if (p == null || p.txid == null || p.txid.trim().isEmpty()) {
            if (callback != null) callback.done(false, "Empty trxId");
            return;
        }

        final String txid = p.txid.trim();
        final Context app = context.getApplicationContext();

        if (isLocallySeen(app, txid)) {
            Log.d(TAG, "Skip local: " + txid);
            if (callback != null) callback.done(true, "Skip (local): " + txid);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("deposits").document(txid);

        ref.get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists()) {
                        markLocal(app, txid);
                        Log.d(TAG, "Skip DB exists: " + txid);
                        if (callback != null) callback.done(true, "Skip (in DB): " + txid);
                        return;
                    }
                    writeNew(app, ref, p, txid, callback);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "get failed, try create: " + e.getMessage());
                    writeNew(app, ref, p, txid, callback);
                });
    }

    private static void writeNew(Context app, DocumentReference ref, PaymentSms p,
                                 String txid, Callback callback) {
        Map<String, Object> data = new HashMap<>();
        try {
            data.put("amount", Double.parseDouble(p.amount));
        } catch (Exception e) {
            if (callback != null) callback.done(false, "Bad amount");
            return;
        }
        data.put("date", new Timestamp(new Date(p.timestamp)));
        data.put("method", p.method);
        data.put("status", "pending");
        data.put("trxId", txid);
        data.put("userEmail", "");
        data.put("userId", "");
        data.put("source", GatewayConfig.SOURCE);
        data.put("used", false);
        data.put("smsSender", p.sender != null ? p.sender : "");

        ref.set(data)
                .addOnSuccessListener(v -> {
                    markLocal(app, txid);
                    Log.d(TAG, "Created: " + txid);
                    if (callback != null) callback.done(true, "Saved: " + txid);
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.toLowerCase().contains("permission")
                            || msg.toLowerCase().contains("already")
                            || msg.toLowerCase().contains("exists")) {
                        markLocal(app, txid);
                        if (callback != null) callback.done(true, "Skip (exists): " + txid);
                    } else {
                        Log.e(TAG, "Fail " + txid + ": " + msg);
                        if (callback != null) callback.done(false, msg);
                    }
                });
    }

    private static SharedPreferences pref(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static boolean isLocallySeen(Context c, String txid) {
        Set<String> set = pref(c).getStringSet(KEY_IDS, null);
        return set != null && set.contains(txid);
    }

    private static void markLocal(Context c, String txid) {
        Set<String> old = pref(c).getStringSet(KEY_IDS, null);
        Set<String> next = new HashSet<>();
        if (old != null) next.addAll(old);
        next.add(txid);
        if (next.size() > 500) {
            int remove = next.size() - 400;
            for (String s : new HashSet<>(next)) {
                if (remove-- <= 0) break;
                next.remove(s);
            }
        }
        pref(c).edit().putStringSet(KEY_IDS, next).apply();
    }
}
