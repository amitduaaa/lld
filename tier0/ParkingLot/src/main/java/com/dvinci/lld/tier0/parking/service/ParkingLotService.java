package com.dvinci.lld.tier0.parking.service;

import com.dvinci.lld.tier0.parking.exception.ActiveParkingSessionException;
import com.dvinci.lld.tier0.parking.exception.NoSpotAvailableException;
import com.dvinci.lld.tier0.parking.model.*;
import com.dvinci.lld.tier0.parking.payment.PaymentRequest;
import com.dvinci.lld.tier0.parking.payment.PaymentResult;
import com.dvinci.lld.tier0.parking.payment.PaymentService;
import com.dvinci.lld.tier0.parking.strategy.PricingStrategy;
import com.dvinci.lld.tier0.parking.strategy.SpotAllocationStrategy;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Application service coordinating parking entry, ticket lifecycle, payment and exit. **/
public final class ParkingLotService {
    private final List<ParkingFloor> floors;
    private final SpotAllocationStrategy spotAllocationStrategy;
    private final PricingStrategy pricingStrategy;
    private final PaymentService paymentService;
    private final Clock clock;
    private final ConcurrentMap<String, ParkingTicket> activeTickets = new ConcurrentHashMap<>();
    private final Set<String> vehiclesWithActiveSessions = ConcurrentHashMap.newKeySet();

    public ParkingLotService(
            List<ParkingFloor> floors,
            SpotAllocationStrategy spotAllocationStrategy,
            PricingStrategy pricingStrategy,
            PaymentService paymentService,
            Clock clock
    ) {
        this.floors = List.copyOf(Objects.requireNonNull(floors, "floors"));
        if (this.floors.isEmpty()) throw new IllegalArgumentException("At least one floor is required");
        long distinctFloors = this.floors.stream().map(ParkingFloor::floorNumber).distinct().count();
        if (distinctFloors != this.floors.size()) throw new IllegalArgumentException("Duplicate floor numbers");
        Set<String> spotIds = new HashSet<>();
        for (ParkingFloor floor : this.floors) {
            for (ParkingSpot spot : floor.spots()) {
                if (!spotIds.add(spot.id())) throw new IllegalArgumentException("Spot IDs must be globally unique: " + spot.id());
            }
        }
        this.spotAllocationStrategy = Objects.requireNonNull(spotAllocationStrategy, "spotAllocationStrategy");
        this.pricingStrategy = Objects.requireNonNull(pricingStrategy, "pricingStrategy");
        this.paymentService = Objects.requireNonNull(paymentService, "paymentService");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ParkingTicket park(Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "vehicle");
        String plateKey = vehicle.licensePlate();
        if (!vehiclesWithActiveSessions.add(plateKey)) {
            throw new ActiveParkingSessionException("Vehicle already has an active parking session: " + plateKey);
        }

        ParkingSpot claimedSpot = null;
        try {
            claimedSpot = spotAllocationStrategy.allocate(vehicle, floors)
                    .orElseThrow(() -> new NoSpotAvailableException("No compatible spot available for " + vehicle.type()));
            ParkingTicket ticket = new ParkingTicket(vehicle, claimedSpot, clock.instant());
            activeTickets.put(ticket.id(), ticket);
            return ticket;
        } catch (RuntimeException exception) {
            if (claimedSpot != null) claimedSpot.release(vehicle);
            vehiclesWithActiveSessions.remove(plateKey);
            throw exception;
        }
    }

    /** Failed payment leaves the ticket active and the spot occupied, allowing a retry. */
    public ExitResult exit(String ticketId) {
        ParkingTicket ticket = activeTickets.get(Objects.requireNonNull(ticketId, "ticketId"));
        if (ticket == null) throw new IllegalArgumentException("No active ticket found: " + ticketId);

        synchronized (ticket) {
            if (ticket.status() != TicketStatus.ACTIVE) throw new IllegalStateException("Ticket is not active");
            Instant exitTime = clock.instant();
            Money fee = pricingStrategy.calculateFee(ticket, exitTime);
            PaymentResult payment = paymentService.pay(new PaymentRequest(ticket.id(), ticket.id(), fee));
            if (!payment.successful()) return new ExitResult(ticket.id(), fee, payment, false);

            if (!ticket.spot().release(ticket.vehicle())) {
                // Payment may already have succeeded. Keep the ticket/session visible for reconciliation.
                throw new IllegalStateException("Payment succeeded but spot release failed; reconciliation required for ticket " + ticket.id());
            }
            ticket.close(exitTime, fee.amount());
            activeTickets.remove(ticket.id(), ticket);
            vehiclesWithActiveSessions.remove(ticket.vehicle().licensePlate());
            return new ExitResult(ticket.id(), fee, payment, true);
        }
    }

    public Optional<ParkingTicket> findActiveTicket(String ticketId) {
        return Optional.ofNullable(activeTickets.get(ticketId));
    }

    public List<ParkingFloor> floors() { return floors; }

    public Map<Integer, Map<SpotType, Long>> availability() {
        Map<Integer, Map<SpotType, Long>> result = new TreeMap<>();
        for (ParkingFloor floor : floors) {
            Map<SpotType, Long> counts = new EnumMap<>(SpotType.class);
            for (SpotType type : SpotType.values()) counts.put(type, floor.availableCount(type));
            result.put(floor.floorNumber(), Collections.unmodifiableMap(counts));
        }
        return Collections.unmodifiableMap(result);
    }
}
