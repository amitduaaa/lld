package com.dvinci.lld.tier0.parking.strategy;

import com.dvinci.lld.tier0.parking.model.Money;
import com.dvinci.lld.tier0.parking.model.ParkingTicket;

import java.time.Instant;

public interface PricingStrategy {
    Money calculateFee(ParkingTicket ticket, Instant exitTime);
}
