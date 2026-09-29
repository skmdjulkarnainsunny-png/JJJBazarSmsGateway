package com.jjjbazar.smsgateway;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * bKash / Nagad / Rocket receive SMS parser.
 *
 * Rocket real format (sender 16216):
 *   Tk30.00 received from A/C:**271 Fee:Tk0, Your A/C Balance: Tk76.54
 *   TxnId:6944542452 Date:12-SEP-26 04:06:36 pm.
 *
 * Reject:
 *   Tk12,300.00 transferred to A/C:**864 ...
 */
public class SmsParser {

    // TxnId:6944542452  /  TxnID: ABC123  /  TrxID: ...
    private static final Pattern TX_PATTERN = Pattern.compile(
            "(?i)\\b(?:TxnID|TrxID|TxID|TxnId|Transaction\\s*ID|TransactionID|Txn\\s*ID)\\s*[:#-]?\\s*([A-Z0-9]{6,30})\\b"
    );

    // Amount: Tk 500  /  Amount: 500
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)\\bAmount\\s*[:#-]?\\s*(?:Tk\\.?|BDT|৳)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"
    );

    // Rocket style: Tk1,320.00 received ...
    private static final Pattern AMOUNT_ROCKET = Pattern.compile(
            "(?i)(?:Tk\\.?|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*received"
    );

    // Generic fallback
    private static final Pattern AMOUNT_ALT = Pattern.compile(
            "(?i)(?:Tk\\.?|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:has been|received|credited|added)?"
    );

    // Rocket short codes
    private static final String[] ROCKET_SENDERS = {
            "16216", "16215", "16214", "rocket"
    };

    private static final String[] BKASH_SENDERS = {
            "bkash", "16267"
    };

    private static final String[] NAGAD_SENDERS = {
            "nagad", "16167"
    };

    private static final String[] RECEIVE_KEYWORDS = {
            "money received",
            "cash in",
            "cash-in",
            "you have received",
            "received from",
            "received tk",
            "received bdt",
            "credited",
            "has been credited",
            "payment received"
    };

    private static final String[] REJECT_KEYWORDS = {
            "send money",
            "cash out",
            "cash-out",
            "payment to",
            "paid to",
            "you have sent",
            "sent tk",
            "sent bdt",
            "money has been sent",
            "transferred to"
    };

    public static PaymentSms parse(String sender, String body, long timestamp) {
        if (body == null || body.trim().isEmpty()) return null;

        String lowerBody = body.toLowerCase(Locale.US);

        // 1) Reject send / transfer type
        for (String bad : REJECT_KEYWORDS) {
            if (lowerBody.contains(bad)) return null;
        }

        // 2) Must look like receive
        boolean isReceive = false;
        for (String good : RECEIVE_KEYWORDS) {
            if (lowerBody.contains(good)) {
                isReceive = true;
                break;
            }
        }
        if (!isReceive && lowerBody.contains("received from")) {
            isReceive = true;
        }
        if (!isReceive && lowerBody.matches("(?s).*\\btk\\.?\\s*[0-9,]+(?:\\.[0-9]{1,2})?\\s*received\\b.*")) {
            isReceive = true;
        }
        if (!isReceive) return null;

        // 3) Sender → method
        String senderName = normalizeSender(sender);
        String method = getMethod(senderName);
        if (method == null) return null;

        // 4) TxnID
        String txid = firstMatch(TX_PATTERN, body);
        if (txid == null) return null;
        txid = txid.trim().toUpperCase(Locale.US);

        // Reject mobile-number looking IDs
        if (txid.matches("^01\\d{9}$")) return null;

        // Rocket TxnId is pure digits (e.g. 6944542452) — allow
        if (method.equals("Rocket")) {
            if (!txid.matches("\\d{6,30}") && !txid.matches(".*\\d.*")) return null;
        } else {
            // bKash / Nagad: must have at least one digit
            if (!txid.matches(".*\\d.*")) return null;
        }

        // 5) Amount — Rocket style first
        String amount = firstMatch(AMOUNT_ROCKET, body);
        if (amount == null) amount = firstMatch(AMOUNT_PATTERN, body);
        if (amount == null) amount = firstMatch(AMOUNT_ALT, body);
        if (amount == null) return null;

        amount = amount.replace(",", "").trim();
        try {
            double value = Double.parseDouble(amount);
            if (value <= 0) return null;
            amount = Math.floor(value) == value
                    ? String.valueOf((long) value)
                    : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }

        return new PaymentSms(method, txid, amount,
                sender == null ? "" : sender.trim(), timestamp);
    }

    private static String getMethod(String sender) {
        if (sender == null || sender.isEmpty()) return null;

        for (String s : ROCKET_SENDERS) {
            if (sender.equals(s) || sender.contains(s)) return "Rocket";
        }
        for (String s : BKASH_SENDERS) {
            if (sender.equals(s) || sender.contains(s)) return "bKash";
        }
        for (String s : NAGAD_SENDERS) {
            if (sender.equals(s) || sender.contains(s)) return "Nagad";
        }
        return null;
    }

    private static String normalizeSender(String sender) {
        if (sender == null) return "";
        String s = sender.trim().toLowerCase(Locale.US);
        if (s.startsWith("+88")) s = s.substring(3);
        if (s.startsWith("88") && s.length() > 10) s = s.substring(2);
        return s.replace(" ", "").replace("-", "").replace("_", "").replace(".", "");
    }

    private static String firstMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
