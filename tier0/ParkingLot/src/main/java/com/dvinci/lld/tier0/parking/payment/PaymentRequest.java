package com.dvinci.lld.tier0.parking.payment;

import com.dvinci.lld.tier0.parking.model.Money;

import java.util.Objects;

public record PaymentRequest(String idempotencyKey, String ticketId, Money amount) {
    public PaymentRequest {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(ticketId, "ticketId");
        Objects.requireNonNull(amount, "amount");
    }
}
