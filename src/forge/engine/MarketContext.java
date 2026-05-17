package forge.engine;

import java.time.LocalDateTime;
import java.util.Objects;

public class MarketContext {
    private final String instrumentSymbol;
    private final LocalDateTime timestamp;
    private final long lastPriceTicks;
    private final double lastPrice;
    private final double tickSize;
    private final double tickDollarValue;
    private final boolean hasOpenPosition;

    public MarketContext(
            String instrumentSymbol,
            LocalDateTime timestamp,
            double lastPrice,
            boolean hasOpenPosition
    ) {
        this(
                instrumentSymbol,
                timestamp,
                Math.round(lastPrice),
                lastPrice,
                1.0,
                1.0,
                hasOpenPosition
        );
    }

    public MarketContext(
            String instrumentSymbol,
            LocalDateTime timestamp,
            long lastPriceTicks,
            double tickSize,
            double tickDollarValue,
            boolean hasOpenPosition
    ) {
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        if (lastPriceTicks <= 0) {
            throw new IllegalArgumentException("lastPriceTicks must be greater than zero");
        }
        this.instrumentSymbol = normalizeInstrumentSymbol(instrumentSymbol);
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
        this.lastPriceTicks = lastPriceTicks;
        this.lastPrice = calculateLastPrice(lastPriceTicks, tickSize);
        this.tickSize = validatePositive(tickSize, "tickSize");
        this.tickDollarValue = validatePositive(tickDollarValue, "tickDollarValue");
        this.hasOpenPosition = hasOpenPosition;
    }

    private MarketContext(
            String instrumentSymbol,
            LocalDateTime timestamp,
            long lastPriceTicks,
            double lastPrice,
            double tickSize,
            double tickDollarValue,
            boolean hasOpenPosition
    ) {
        if (lastPriceTicks <= 0) {
            throw new IllegalArgumentException("lastPriceTicks must be greater than zero");
        }
        this.instrumentSymbol = normalizeInstrumentSymbol(instrumentSymbol);
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
        this.lastPriceTicks = lastPriceTicks;
        this.lastPrice = validatePositive(lastPrice, "lastPrice");
        this.tickSize = validatePositive(tickSize, "tickSize");
        this.tickDollarValue = validatePositive(tickDollarValue, "tickDollarValue");
        this.hasOpenPosition = hasOpenPosition;
    }

    private String normalizeInstrumentSymbol(String instrumentSymbol) {
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        return instrumentSymbol.trim().toUpperCase();
    }

    private double calculateLastPrice(long lastPriceTicks, double tickSize) {
        validatePositive(tickSize, "tickSize");
        return lastPriceTicks * tickSize;
    }

    private double validatePositive(double value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
        return value;
    }

    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public long getLastPriceTicks() {
        return lastPriceTicks;
    }

    public double getTickSize() {
        return tickSize;
    }

    public double getTickDollarValue() {
        return tickDollarValue;
    }

    public boolean hasOpenPosition() {
        return hasOpenPosition;
    }
}
