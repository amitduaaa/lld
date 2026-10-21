package com.dvinci.lld.tier0.parking;

import com.dvinci.lld.tier0.parking.model.*;
import com.dvinci.lld.tier0.parking.payment.InMemoryPaymentService;
import com.dvinci.lld.tier0.parking.service.ExitResult;
import com.dvinci.lld.tier0.parking.service.ParkingLotService;
import com.dvinci.lld.tier0.parking.strategy.FirstFitAllocationStrategy;
import com.dvinci.lld.tier0.parking.strategy.HourlyPricingStrategy;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        ParkingFloor ground = new ParkingFloor(0, List.of(
                new ParkingSpot("G-M1", 0, SpotType.MOTORCYCLE),
                new ParkingSpot("G-C1", 0, SpotType.COMPACT),
                new ParkingSpot("G-L1", 0, SpotType.LARGE)));
        ParkingFloor first = new ParkingFloor(1, List.of(
                new ParkingSpot("F1-C1", 1, SpotType.COMPACT),
                new ParkingSpot("F1-L1", 1, SpotType.LARGE)));

        var pricing = new HourlyPricingStrategy("INR", Map.of(
                SpotType.MOTORCYCLE, new BigDecimal("20.00"),
                SpotType.COMPACT, new BigDecimal("40.00"),
                SpotType.LARGE, new BigDecimal("70.00")));
        var parking = new ParkingLotService(List.of(ground, first), new FirstFitAllocationStrategy(),
                pricing, new InMemoryPaymentService(), Clock.systemUTC());

        ParkingTicket ticket = parking.park(new Vehicle("KA01AB1234", VehicleType.CAR));
        System.out.printf("Ticket %s: vehicle %s parked at %s on floor %d%n",
                ticket.id(), ticket.vehicle().licensePlate(), ticket.spot().id(), ticket.spot().floorNumber());
        System.out.println("Availability: " + parking.availability());

        ExitResult exit = parking.exit(ticket.id());
        System.out.printf("Exit successful=%s, fee=%s %s%n", exit.spotReleased(), exit.fee().currency(), exit.fee().amount());
        System.out.println("Availability after exit: " + parking.availability());
    }
}