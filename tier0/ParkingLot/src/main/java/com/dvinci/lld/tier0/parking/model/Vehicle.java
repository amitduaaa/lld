package com.dvinci.lld.tier0.parking.model;

import java.util.Objects;

public record Vehicle(String licensePlate, VehicleType type) {
    public Vehicle {
        licensePlate = Objects.requireNonNull(licensePlate, "licensePlate").trim().toUpperCase();
        if (licensePlate.isBlank()) throw new IllegalArgumentException("License plate cannot be blank");
        Objects.requireNonNull(type, "type");
    }
}
