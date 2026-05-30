package forge.engine;

import forge.model.FuturesInstrumentSpec;

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
        /*
         * Intent: Create a legacy market context from display-price input.
         * Precondition: Instrument symbol, timestamp, and price must be valid.
         * Returns: A constructed MarketContext instance.
         * Postcondition: Price is rounded into tick space using default one-point tick metadata.
         */
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
        /*
         * Intent: Create a strategy-facing market context from tick-normalized market data.
         * Precondition: Instrument symbol, timestamp, tick price, tick size, and tick dollar value must be valid.
         * Returns: A constructed MarketContext instance.
         * Postcondition: Display price is derived from ticks and tick size.
         */
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
        /*
         * Intent: Create a market context when both display price and tick price are already known.
         * Precondition: Instrument symbol, timestamp, price values, tick size, and tick value must be valid.
         * Returns: A constructed MarketContext instance.
         * Postcondition: Context is immutable and ready for strategy evaluation.
         */
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
        /*
         * Intent: Validate and normalize instrument symbols for strategy-facing context.
         * Precondition: Symbol must be nonblank.
         * Returns: Trimmed uppercase symbol.
         * Postcondition: Invalid symbols fail before context construction completes.
         */
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        return instrumentSymbol.trim().toUpperCase();
    }

    private double calculateLastPrice(long lastPriceTicks, double tickSize) {
        /*
         * Intent: Convert integer tick price into display price for strategy/reporting compatibility.
         * Precondition: Last price ticks and tick size must be positive.
         * Returns: Display price using the same tick/fixed-point conversion as reporting.
         * Postcondition: No context state is changed.
         */
        validatePositive(tickSize, "tickSize");
        return FuturesInstrumentSpec.displayPrice(lastPriceTicks, tickSize);
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
