package forge.engine;

import java.time.LocalDateTime;
import java.util.Objects;

public class MarketContext {
    private final String instrumentSymbol;
    private final LocalDateTime timestamp;
    private final double lastPrice;
    private final boolean hasOpenPosition;

    public MarketContext(
            String instrumentSymbol,
            LocalDateTime timestamp,
            double lastPrice,
            boolean hasOpenPosition
    ) {
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        if (lastPrice <= 0) {
            throw new IllegalArgumentException("lastPrice must be greater than zero");
        }

        this.instrumentSymbol = instrumentSymbol.trim().toUpperCase();
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
        this.lastPrice = lastPrice;
        this.hasOpenPosition = hasOpenPosition;
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

    public boolean hasOpenPosition() {
        return hasOpenPosition;
    }
}
