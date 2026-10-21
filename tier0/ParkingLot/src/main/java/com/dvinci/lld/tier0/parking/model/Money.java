package com.dvinci.lld.tier0.parking.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(String currency, BigDecimal amount) {
    public Money {
        currency = Objects.requireNonNull(currency, "currency").toUpperCase();
        amount = Objects.requireNonNull(amount, "amount").setScale(2, RoundingMode.HALF_UP);

        if (currency.isBlank()) throw new IllegalArgumentException("Currency cannot be blank");
        if (amount.signum() < 0) throw new IllegalArgumentException("Money cannot be negative");
    }

    public static Money of(String currency, String amount) {
        return new Money(currency, new BigDecimal(amount));
    }
}
