package com.jjjbazar.smsgateway;

/**
 * Secret source marker written to every SMS deposit.
 * Must match Firestore rules + website (verifyAutoPayment).
 * Do NOT use the old weak value "sms_gateway".
 */
public final class GatewayConfig {
    private GatewayConfig() {}

    /** Hard-to-guess source token */
    public static final String SOURCE = "jjj_sg_x7K9mQ2pL8nR4wV1sT6";

    /** Default scan interval (seconds) */
    public static final int DEFAULT_SCAN_SEC = 120;

    /** Allowed range for UI setting */
    public static final int MIN_SCAN_SEC = 60;
    public static final int MAX_SCAN_SEC = 900; // 15 min
}
