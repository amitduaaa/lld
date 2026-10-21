package com.dvinci.lld.tier0.parking.model;

import java.util.List;
import java.util.Objects;

public final class ParkingFloor {
    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int floorNumber, List<ParkingSpot> spots) {
        if (floorNumber < 0) throw new IllegalArgumentException("Floor number must be >= 0");
        this.floorNumber = floorNumber;
        this.spots = List.copyOf(Objects.requireNonNull(spots, "spots"));
        if (this.spots.stream().anyMatch(s -> s.floorNumber() != floorNumber)) {
            throw new IllegalArgumentException("Every spot must belong to this floor");
        }
        long distinct = this.spots.stream().map(ParkingSpot::id).distinct().count();
        if (distinct != this.spots.size()) {
            throw new IllegalArgumentException("Duplicate spot Ids on floor");
        }
    }

    public int floorNumber() { return floorNumber; }
    public List<ParkingSpot> spots() { return spots; }
    public long availableCount() { return spots.stream().filter(ParkingSpot::isAvailable).count(); }
    public long availableCount(SpotType type) {
        return spots.stream().filter(s -> s.type() == type && s.isAvailable()).count();
    }
}
