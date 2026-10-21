package com.dvinci.lld.tier0.parking.strategy;

import com.dvinci.lld.tier0.parking.model.Money;
import com.dvinci.lld.tier0.parking.model.ParkingTicket;
import com.dvinci.lld.tier0.parking.model.SpotType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Bills each started hour, with a minimum of one hour. Rates are per hour.  **/
public final class HourlyPricingStrategy implements PricingStrategy {
    private final String currency;
    private final Map<SpotType, BigDecimal> hourlyRates;

    public HourlyPricingStrategy(String currency, Map<SpotType, BigDecimal> hourlyRates) {
        this.currency = Objects.requireNonNull(currency, "currency").toUpperCase();
        this.hourlyRates = new EnumMap<>(SpotType.class);
        this.hourlyRates.putAll(Objects.requireNonNull(hourlyRates, "hourlyRates"));

        for (SpotType type : SpotType.values()) {
            BigDecimal rate = this.hourlyRates.get(type);
            if (rate == null || rate.signum() < 0) throw new IllegalArgumentException("Missing or invalid rate for " + type);
        }
    }

    @Override
    public Money calculateFee(ParkingTicket ticket, Instant exitTime) {
        Objects.requireNonNull(ticket, "ticket");
        Objects.requireNonNull(exitTime, "exitTime");

        if (exitTime.isBefore(ticket.entryTime())) throw new IllegalArgumentException("Exit time precedes entry time");
        long seconds = Duration.between(ticket.entryTime(), exitTime).getSeconds();
        long billedHours = Math.max(1, (seconds + 3559) / 3600);
        BigDecimal amount = hourlyRates.get(ticket.spot().type())
                .multiply(BigDecimal.valueOf(billedHours)).setScale(2, RoundingMode.HALF_UP);
        return new Money(currency, amount);
    }
}
