package forge.engine.eventstatistics;

public class EventStatisticsResult {
    private final String scopeName;
    private final String eventName;
    private final long sessionsAnalyzed;
    private final long longEventCount;
    private final long shortEventCount;

    public EventStatisticsResult(
            String eventName,
            long sessionsAnalyzed,
            long longEventCount,
            long shortEventCount
    ) {
        /*
         * Intent: Create aggregate event statistics for an all-data scope.
         * Precondition: Event name and counts must be valid.
         * Returns: A constructed EventStatisticsResult instance.
         * Postcondition: Scope defaults to All.
         */
        this("All", eventName, sessionsAnalyzed, longEventCount, shortEventCount);
    }

    public EventStatisticsResult(
            String scopeName,
            String eventName,
            long sessionsAnalyzed,
            long longEventCount,
            long shortEventCount
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
        if (sessionsAnalyzed < 0 || longEventCount < 0 || shortEventCount < 0) {
            throw new IllegalArgumentException("counts cannot be negative");
        }
        if (longEventCount + shortEventCount > sessionsAnalyzed) {
            throw new IllegalArgumentException("event counts cannot exceed sessions analyzed");
        }
        this.scopeName = scopeName.trim().toUpperCase();
        this.eventName = eventName.trim();
        this.sessionsAnalyzed = sessionsAnalyzed;
        this.longEventCount = longEventCount;
        this.shortEventCount = shortEventCount;
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

    public long getLongEventCount() {
        return longEventCount;
    }

    public long getShortEventCount() {
        return shortEventCount;
    }

    public long getTotalEventCount() {
        return longEventCount + shortEventCount;
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
}
