package forge.engine.eventstatistics;

import java.io.Serial;
import java.io.Serializable;

public class EventStatisticsResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String scopeName;
    private final String eventName;
    private final long sessionsAnalyzed;
    private final long highEventCount;
    private final long lowEventCount;
    private final double averageOvernightRangeTicks;
    private final double averageFirstHourRangeTicks;
    private final double averageRthRangeTicks;
    private final double averageOvernightVolume;
    private final double averageFirstHourVolume;
    private final double averageRthVolume;

    public EventStatisticsResult(
            String eventName,
            long sessionsAnalyzed,
            long highEventCount,
            long lowEventCount
    ) {
        /*
         * Intent: Create aggregate event statistics for an all-data scope.
         * Precondition: Event name and counts must be valid.
         * Returns: A constructed EventStatisticsResult instance.
         * Postcondition: Scope defaults to All.
         */
        this("All", eventName, sessionsAnalyzed, highEventCount, lowEventCount);
    }

    public EventStatisticsResult(
            String scopeName,
            String eventName,
            long sessionsAnalyzed,
            long highEventCount,
            long lowEventCount
    ) {
        this(
                scopeName,
                eventName,
                sessionsAnalyzed,
                highEventCount,
                lowEventCount,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }

    public EventStatisticsResult(
            String scopeName,
            String eventName,
            long sessionsAnalyzed,
            long highEventCount,
            long lowEventCount,
            double averageOvernightRangeTicks,
            double averageFirstHourRangeTicks,
            double averageRthRangeTicks,
            double averageOvernightVolume,
            double averageFirstHourVolume,
            double averageRthVolume
    ) {
        /*
         * Intent: Store occurrence counts for one instrument or contract statistics scope.
         * Precondition: Scope/event names must be nonblank and event counts cannot exceed sessions analyzed.
         * Returns: A constructed EventStatisticsResult instance.
         * Postcondition: Scope name is normalized and rates can be derived from counts.
         */
        if (scopeName == null || scopeName.trim().isEmpty()) {
            throw new IllegalArgumentException("scopeName is required");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        if (sessionsAnalyzed < 0 || highEventCount < 0 || lowEventCount < 0) {
            throw new IllegalArgumentException("counts cannot be negative");
        }
        if (highEventCount + lowEventCount > sessionsAnalyzed) {
            throw new IllegalArgumentException("event counts cannot exceed sessions analyzed");
        }
        this.scopeName = scopeName.trim().toUpperCase();
        this.eventName = eventName.trim();
        this.sessionsAnalyzed = sessionsAnalyzed;
        this.highEventCount = highEventCount;
        this.lowEventCount = lowEventCount;
        this.averageOvernightRangeTicks = validateAverage(averageOvernightRangeTicks, "averageOvernightRangeTicks");
        this.averageFirstHourRangeTicks = validateAverage(averageFirstHourRangeTicks, "averageFirstHourRangeTicks");
        this.averageRthRangeTicks = validateAverage(averageRthRangeTicks, "averageRthRangeTicks");
        this.averageOvernightVolume = validateAverage(averageOvernightVolume, "averageOvernightVolume");
        this.averageFirstHourVolume = validateAverage(averageFirstHourVolume, "averageFirstHourVolume");
        this.averageRthVolume = validateAverage(averageRthVolume, "averageRthVolume");
    }

    private double validateAverage(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be a finite non-negative value");
        }
        return value;
    }

    public String getScopeName() {
        return scopeName;
    }

    public String getEventName() {
        return eventName;
    }

    public long getSessionsAnalyzed() {
        return sessionsAnalyzed;
    }

    public long getHighEventCount() {
        return highEventCount;
    }

    public long getLowEventCount() {
        return lowEventCount;
    }

    public long getTotalEventCount() {
        return highEventCount + lowEventCount;
    }

    public long getNoEventCount() {
        return sessionsAnalyzed - getTotalEventCount();
    }

    public double getEventRate() {
        /*
         * Intent: Calculate the share of analyzed sessions where the event occurred.
         * Precondition: Result counts must be validated.
         * Returns: Event rate from 0.0 to 1.0, or 0.0 when no sessions were analyzed.
         * Postcondition: Result state is unchanged.
         */
        if (sessionsAnalyzed == 0) {
            return 0;
        }
        return (double) getTotalEventCount() / sessionsAnalyzed;
    }

    public double getAverageOvernightRangeTicks() {
        return averageOvernightRangeTicks;
    }

    public double getAverageFirstHourRangeTicks() {
        return averageFirstHourRangeTicks;
    }

    public double getAverageRthRangeTicks() {
        return averageRthRangeTicks;
    }

    public double getAverageOvernightVolume() {
        return averageOvernightVolume;
    }

    public double getAverageFirstHourVolume() {
        return averageFirstHourVolume;
    }

    public double getAverageRthVolume() {
        return averageRthVolume;
    }

    public boolean hasSupportingAverages() {
        return averageOvernightRangeTicks > 0
                || averageFirstHourRangeTicks > 0
                || averageRthRangeTicks > 0
                || averageOvernightVolume > 0
                || averageFirstHourVolume > 0
                || averageRthVolume > 0;
    }
}
