package com.dvinci.lld.tier0.parking.strategy;

import com.dvinci.lld.tier0.parking.model.ParkingFloor;
import com.dvinci.lld.tier0.parking.model.ParkingSpot;
import com.dvinci.lld.tier0.parking.model.Vehicle;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Deterministic first-fit policy; lower floor first, then spot ID. **/
public class FirstFitAllocationStrategy implements SpotAllocationStrategy {
    @Override
    public Optional<ParkingSpot> allocate(Vehicle vehicle, List<ParkingFloor> floors) {
        return floors.stream().sorted(Comparator.comparingInt(ParkingFloor::floorNumber))
                .flatMap(floor -> floor.spots().stream().sorted(Comparator.comparing(ParkingSpot::id)))
                .filter(spot -> spot.canFit(vehicle))
                .filter(ParkingSpot::isAvailable)
                .filter(spot -> spot.tryAssign(vehicle))
                .findFirst();
    }
}
