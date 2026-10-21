package com.dvinci.lld.tier0.parking.strategy;

import com.dvinci.lld.tier0.parking.model.ParkingFloor;
import com.dvinci.lld.tier0.parking.model.ParkingSpot;
import com.dvinci.lld.tier0.parking.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface SpotAllocationStrategy {
    /** Returns a spot only after atomically claiming it for the vehicle. **/
    Optional<ParkingSpot> allocate(Vehicle vehicle, List<ParkingFloor> floors);
}
