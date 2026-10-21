package com.dvinci.lld.tier0.parking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ParkingTicket {
    // Final fields are naturally thread-safe. The Java Memory Model guarantees
    // they are instantly visible to all threads once the constructor completes.
    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Instant entryTime;

    // Mutable fields require synchronization to prevent race conditions
    // and stale cache reads across multiple threads.
    private TicketStatus status = TicketStatus.ACTIVE;
    private Instant exitTime;
    private BigDecimal amountPaid;

    public ParkingTicket(Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
        this.id = UUID.randomUUID().toString();
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.spot = Objects.requireNonNull(spot, "spot");
        this.entryTime = Objects.requireNonNull(entryTime, "entryTime");
    }

    // Synchronized guarantees atomicity for this check-then-act compound action.
    // It prevents two checkout kiosks from closing the same ticket simultaneously.
    public synchronized void close(Instant exitTime, BigDecimal amountPaid) {
        if (status != TicketStatus.ACTIVE) throw new IllegalArgumentException("Ticket is already closed");
        if (exitTime.isBefore(entryTime)) throw new IllegalArgumentException("Exit time precedes entry time");
        this.exitTime = Objects.requireNonNull(exitTime, "exitTime");
        this.amountPaid = Objects.requireNonNull(amountPaid, "amountPaid");
        this.status = TicketStatus.CLOSED;
    }

    // No synchronization needed: reading final fields is always thread-safe.
    public String id() { return id; }
    public Vehicle vehicle() { return vehicle; }
    public ParkingSpot spot() { return spot; }
    public Instant entryTime() { return entryTime; }

    // Synchronized ensures memory visibility.
    // It forces the thread to read the latest value from main memory,
    // preventing it from reading a stale value from its local CPU cache.
    public synchronized TicketStatus status() { return status; }
    public synchronized Instant exitTime() { return exitTime; }
    public synchronized BigDecimal amountPaid() { return amountPaid; }
}