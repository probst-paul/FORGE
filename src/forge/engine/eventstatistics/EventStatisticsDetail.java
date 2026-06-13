package forge.engine.eventstatistics;

import forge.event.EventSide;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

public class EventStatisticsDetail implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String contractSymbol;
    private final LocalDate sessionDate;
    private final String eventName;
    private final EventSide side;
    private final Instant eventTime;
    private final long eventPriceTicks;
    private final long overnightLowTicks;
    private final long overnightHighTicks;
    private final long firstHourLowTicks;
    private final long firstHourHighTicks;
    private final long rthLowTicks;
    private final long rthHighTicks;
    private final long overnightVolume;
    private final long firstHourVolume;
    private final long rthVolume;
    private final long overnightTradeCount;
    private final long firstHourTradeCount;
    private final long rthTradeCount;

    public EventStatisticsDetail(
            String contractSymbol,
            LocalDate sessionDate,
            String eventName,
            EventSide side,
            Instant eventTime,
            long eventPriceTicks,
            long overnightLowTicks,
            long overnightHighTicks,
            long firstHourLowTicks,
            long firstHourHighTicks,
            long rthLowTicks,
            long rthHighTicks,
            long overnightVolume,
            long firstHourVolume,
            long rthVolume,
            long overnightTradeCount,
            long firstHourTradeCount,
            long rthTradeCount
    ) {
        /*
         * Intent: Store one event occurrence with its derived session context.
         * Precondition: Identity, event, and feature values must describe one contract trading day.
         * Returns: A constructed EventStatisticsDetail.
         * Postcondition: Detail values are immutable and ready for GUI/report rendering.
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
        if (side == null) {
            throw new IllegalArgumentException("side is required");
        }
        if (eventTime == null) {
            throw new IllegalArgumentException("eventTime is required");
        }
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.sessionDate = sessionDate;
        this.eventName = eventName.trim();
        this.side = side;
        this.eventTime = eventTime;
        this.eventPriceTicks = eventPriceTicks;
        this.overnightLowTicks = overnightLowTicks;
        this.overnightHighTicks = overnightHighTicks;
        this.firstHourLowTicks = firstHourLowTicks;
        this.firstHourHighTicks = firstHourHighTicks;
        this.rthLowTicks = rthLowTicks;
        this.rthHighTicks = rthHighTicks;
        this.overnightVolume = overnightVolume;
        this.firstHourVolume = firstHourVolume;
        this.rthVolume = rthVolume;
        this.overnightTradeCount = overnightTradeCount;
        this.firstHourTradeCount = firstHourTradeCount;
        this.rthTradeCount = rthTradeCount;
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

    public EventSide getSide() {
        return side;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public long getEventPriceTicks() {
        return eventPriceTicks;
    }

    public long getOvernightLowTicks() {
        return overnightLowTicks;
    }

    public long getOvernightHighTicks() {
        return overnightHighTicks;
    }

    public long getFirstHourLowTicks() {
        return firstHourLowTicks;
    }

    public long getFirstHourHighTicks() {
        return firstHourHighTicks;
    }

    public long getRthLowTicks() {
        return rthLowTicks;
    }

    public long getRthHighTicks() {
        return rthHighTicks;
    }

    public long getOvernightRangeTicks() {
        return overnightHighTicks - overnightLowTicks;
    }

    public long getFirstHourRangeTicks() {
        return firstHourHighTicks - firstHourLowTicks;
    }

    public long getRthRangeTicks() {
        return rthHighTicks - rthLowTicks;
    }

    public long getOvernightVolume() {
        return overnightVolume;
    }

    public long getFirstHourVolume() {
        return firstHourVolume;
    }

    public long getRthVolume() {
        return rthVolume;
    }

    public long getOvernightTradeCount() {
        return overnightTradeCount;
    }

    public long getFirstHourTradeCount() {
        return firstHourTradeCount;
    }

    public long getRthTradeCount() {
        return rthTradeCount;
    }
}
