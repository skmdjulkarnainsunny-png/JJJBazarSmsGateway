package com.jjjbazar.smsgateway;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Uploader {
    public interface Callback {
        void done(boolean ok, String message);
    }

    public static void upload(android.content.Context context, PaymentSms p, Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("amount", Double.parseDouble(p.amount));
        data.put("date", new Timestamp(new Date(p.timestamp)));
        data.put("method", p.method);
        data.put("status", "pending");
        data.put("trxId", p.txid);
        data.put("userEmail", "");
        data.put("userId", "");
        data.put("source", "sms_gateway");
        data.put("used", false);
        data.put("smsSender", p.sender);

        // Using TxnID as document id prevents the same SMS from creating duplicates.
        db.collection("deposits")
                .document(p.txid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.done(true, "Saved: " + p.txid);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.done(false, e.getMessage());
                });
    }
}
