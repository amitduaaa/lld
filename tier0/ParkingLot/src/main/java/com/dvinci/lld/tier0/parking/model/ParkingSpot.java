package com.dvinci.lld.tier0.parking.model;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class ParkingSpot {
    private final String id;
    private final int floorNumber;
    private final SpotType type;
    // Why not use synchronized keyword in method for this instead of Atomic Reference?
    // 1. synchronized keyword forces thread to line up and wait for their turn
    // 2. However, "AtomicReference" uses non-blocking approach. It is significantly faster and more scalable for high-traffic apps
    // 3. Why? Because thread doesn't get frozen waiting for the lock to be released.
    private final AtomicReference<Vehicle> occupant = new AtomicReference<>();

    public ParkingSpot(String id, int floorNumber, SpotType type) {
        this.id = Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("Spot Id cannot be blank");
        if (floorNumber < 0) throw new IllegalArgumentException("Floor number must be >= 0");
        this.floorNumber = floorNumber;
        this.type = Objects.requireNonNull(type, "type");
    }

    public boolean canFit(Vehicle vehicle) {
        return switch (vehicle.type()) {
            case MOTORCYCLE -> true;
            case CAR -> type == SpotType.COMPACT || type == SpotType.LARGE;
            case TRUCK -> type == SpotType.LARGE;
        };
    }

    public boolean tryAssign(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        return canFit(vehicle) && occupant.compareAndSet(null, vehicle);
    }


    public boolean release(Vehicle vehicle) {
        return occupant.compareAndSet(vehicle, null);
    }

    public boolean isAvailable() {
        return occupant.get() == null;
    }

    public Optional<Vehicle> occupant() {
        return Optional.ofNullable(occupant.get());
    }

    public String id() { return id; }
    public int floorNumber() { return floorNumber; }
    public SpotType type() { return type; }
}
