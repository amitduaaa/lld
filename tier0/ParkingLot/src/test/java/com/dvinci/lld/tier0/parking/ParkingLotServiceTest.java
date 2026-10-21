package com.dvinci.lld.tier0.parking;

import com.dvinci.lld.tier0.parking.exception.ActiveParkingSessionException;
import com.dvinci.lld.tier0.parking.exception.NoSpotAvailableException;
import com.dvinci.lld.tier0.parking.model.*;
import com.dvinci.lld.tier0.parking.payment.InMemoryPaymentService;
import com.dvinci.lld.tier0.parking.payment.PaymentResult;
import com.dvinci.lld.tier0.parking.payment.PaymentService;
import com.dvinci.lld.tier0.parking.service.ParkingLotService;
import com.dvinci.lld.tier0.parking.strategy.FirstFitAllocationStrategy;
import com.dvinci.lld.tier0.parking.strategy.HourlyPricingStrategy;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class ParkingLotServiceTest {
    private static HourlyPricingStrategy pricing() {
        return new HourlyPricingStrategy("INR", Map.of(
                SpotType.MOTORCYCLE, new BigDecimal("20"), SpotType.COMPACT, new BigDecimal("40"), SpotType.LARGE, new BigDecimal("70")));
    }

    private static ParkingLotService service(List<ParkingFloor> floors, Clock clock, PaymentService payment) {
        return new ParkingLotService(floors, new FirstFitAllocationStrategy(), pricing(), payment, clock);
    }

    @Test void carGetsCompactSpotAndPaymentReleasesIt() {
        var spot = new ParkingSpot("C1", 0, SpotType.COMPACT);
        var service = service(List.of(new ParkingFloor(0, List.of(spot))),
                Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC), new InMemoryPaymentService());
        var ticket = service.park(new Vehicle("KA01AB1234", VehicleType.CAR));
        assertFalse(spot.isAvailable());
        var exit = service.exit(ticket.id());
        assertTrue(exit.spotReleased());
        assertTrue(spot.isAvailable());
        assertEquals(TicketStatus.CLOSED, ticket.status());
        assertEquals(new BigDecimal("40.00"), ticket.amountPaid());
    }

    @Test void truckCannotUseCompactSpot() {
        var spot = new ParkingSpot("C1", 0, SpotType.COMPACT);
        var service = service(List.of(new ParkingFloor(0, List.of(spot))), Clock.systemUTC(), new InMemoryPaymentService());
        assertThrows(NoSpotAvailableException.class, () -> service.park(new Vehicle("TRUCK1", VehicleType.TRUCK)));
        assertTrue(spot.isAvailable());
    }

    @Test void duplicateActiveSessionForSamePlateIsRejected() {
        var spot1 = new ParkingSpot("C1", 0, SpotType.COMPACT);
        var spot2 = new ParkingSpot("C2", 0, SpotType.COMPACT);
        var service = service(List.of(new ParkingFloor(0, List.of(spot1, spot2))), Clock.systemUTC(), new InMemoryPaymentService());
        service.park(new Vehicle("KA01AB1234", VehicleType.CAR));
        assertThrows(ActiveParkingSessionException.class, () -> service.park(new Vehicle("ka01ab1234", VehicleType.CAR)));
    }

    @Test
    void failedPaymentKeepsTicketAndSpotActive() {
        PaymentService failing = request -> PaymentResult.failure("Card declined");
        var spot = new ParkingSpot("C1", 0, SpotType.COMPACT);
        var service = service(List.of(new ParkingFloor(0, List.of(spot))), Clock.systemUTC(), failing);
        var ticket = service.park(new Vehicle("KA01AB1234", VehicleType.CAR));
        var result = service.exit(ticket.id());
        assertFalse(result.spotReleased());
        assertFalse(spot.isAvailable());
        assertEquals(TicketStatus.ACTIVE, ticket.status());
        assertTrue(service.findActiveTicket(ticket.id()).isPresent());
    }

    @Test void concurrentVehiclesCannotClaimSameSpot() throws Exception {
        var spot = new ParkingSpot("C1", 0, SpotType.COMPACT);
        var service = service(List.of(new ParkingFloor(0, List.of(spot))), Clock.systemUTC(), new InMemoryPaymentService());
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> attempt1 = () -> attempt(service, "CAR1", ready, start);
        Callable<Boolean> attempt2 = () -> attempt(service, "CAR2", ready, start);
        var f1 = pool.submit(attempt1); var f2 = pool.submit(attempt2);
        ready.await(); start.countDown();
        int successes = (f1.get() ? 1 : 0) + (f2.get() ? 1 : 0);
        pool.shutdownNow();
        assertEquals(1, successes);
        assertFalse(spot.isAvailable());
    }

    private boolean attempt(ParkingLotService service, String plate, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown(); start.await();
        try { service.park(new Vehicle(plate, VehicleType.CAR)); return true; }
        catch (NoSpotAvailableException e) { return false; }
    }
}
