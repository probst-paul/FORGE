package forge.condition;

import java.time.Instant;
import java.time.LocalDate;

public class MarketConditionOccurrence {
    private final String contractSymbol;
    private final LocalDate sessionDate;
    private final String eventName;
    private final int eventVersion;
    private final ConditionSide side;
    private final Instant eventTime;
    private final long eventPriceTicks;

    public MarketConditionOccurrence(
            String contractSymbol,
            LocalDate sessionDate,
            String eventName,
            int eventVersion,
            ConditionSide side,
            Instant eventTime,
            long eventPriceTicks
    ) {
        /*
         * Intent: Record one detected market condition occurrence for derived data and statistics.
         * Precondition: Contract/session/event identity, side, timestamp, and price must be valid.
         * Returns: A constructed MarketConditionOccurrence instance.
         * Postcondition: Occurrence is immutable and contract symbol/event name are normalized.
         */
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
        }
        if (sessionDate == null) {
            throw new IllegalArgumentException("sessionDate is required");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        if (eventVersion < 1) {
            throw new IllegalArgumentException("eventVersion must be positive");
        }
        if (side == null) {
            throw new IllegalArgumentException("side is required");
        }
        if (eventTime == null) {
            throw new IllegalArgumentException("eventTime is required");
        }
        if (eventPriceTicks <= 0) {
            throw new IllegalArgumentException("eventPriceTicks must be greater than zero");
        }
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.sessionDate = sessionDate;
        this.eventName = eventName.trim();
        this.eventVersion = eventVersion;
        this.side = side;
        this.eventTime = eventTime;
        this.eventPriceTicks = eventPriceTicks;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public String getEventName() {
        return eventName;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public ConditionSide getSide() {
        return side;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public long getEventPriceTicks() {
        return eventPriceTicks;
    }
}
