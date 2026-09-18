const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
admin.initializeApp({
  databaseURL: "https://saad-topup-default-rtdb.asia-southeast1.firebasedatabase.app"
});

exports.smsPaymentIngest = onRequest({ cors: false }, async (req, res) => {
  try {
    if (req.method !== "POST") return res.status(405).json({ ok: false, error: "POST only" });

    const expectedToken = process.env.DEVICE_TOKEN || "";
    const gotToken = req.get("X-Device-Token") || "";
    if (!expectedToken || gotToken !== expectedToken) {
      return res.status(401).json({ ok: false, error: "Unauthorized" });
    }

    const { method, txid, amount, sender, timestamp } = req.body || {};
    if (!["bKash", "Nagad", "Rocket"].includes(method)) {
      return res.status(400).json({ ok: false, error: "Invalid method" });
    }

    const cleanTx = String(txid || "").trim().toUpperCase();
    const cleanAmount = Number(amount);
    if (!/^[A-Z0-9]{6,30}$/.test(cleanTx) || !Number.isFinite(cleanAmount) || cleanAmount <= 0) {
      return res.status(400).json({ ok: false, error: "Invalid payment data" });
    }

    const ref = admin.database().ref(`XNXANIKPAY/${cleanTx}`);
    const existing = await ref.get();
    if (existing.exists()) {
      return res.status(200).json({ ok: true, duplicate: true });
    }

    await ref.set({
      txid: cleanTx,
      amount: cleanAmount,
      method,
      sender: String(sender || "").slice(0, 50),
      timestamp: Number(timestamp || Date.now()),
      createdAt: admin.database.ServerValue.TIMESTAMP
    });

    return res.status(200).json({ ok: true });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ ok: false, error: "Server error" });
  }
});
