package com.jjjbazar.smsgateway;

public class PaymentSms {
    public String method;
    public String txid;
    public String amount;
    public String sender;
    public long timestamp;

    public PaymentSms(String method, String txid, String amount, String sender, long timestamp) {
        this.method = method;
        this.txid = txid;
        this.amount = amount;
        this.sender = sender;
        this.timestamp = timestamp;
    }
}
