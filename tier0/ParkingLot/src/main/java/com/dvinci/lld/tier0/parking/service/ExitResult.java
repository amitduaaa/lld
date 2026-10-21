package com.dvinci.lld.tier0.parking.service;

import com.dvinci.lld.tier0.parking.model.Money;
import com.dvinci.lld.tier0.parking.payment.PaymentResult;

import java.util.Objects;

public record ExitResult(String ticketId, Money fee, PaymentResult paymentResult, boolean spotReleased) {
    public ExitResult {
        Objects.requireNonNull(ticketId);
        Objects.requireNonNull(fee);
        Objects.requireNonNull(paymentResult);
    }
}
