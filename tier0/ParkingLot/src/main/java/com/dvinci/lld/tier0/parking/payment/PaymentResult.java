package com.dvinci.lld.tier0.parking.payment;

public record PaymentResult(boolean successful, String transactionId, String message) {
    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, "Payment Successful");
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, null, message);
    }
}
