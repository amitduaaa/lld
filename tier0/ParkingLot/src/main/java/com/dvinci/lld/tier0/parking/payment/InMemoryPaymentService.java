package com.dvinci.lld.tier0.parking.payment;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Demo adapter only: simulates successful payments and remembers idempotency keys in this JVM. **/
public final class InMemoryPaymentService implements PaymentService {
    private final ConcurrentMap<String, PaymentResult> results = new ConcurrentHashMap<>();

    @Override
    public PaymentResult pay(PaymentRequest request) {
        return results.computeIfAbsent(request.idempotencyKey(),
                ignored -> PaymentResult.success(UUID.randomUUID().toString()));
    }
}
