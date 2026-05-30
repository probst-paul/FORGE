package forge.engine;

public class EventStatisticsQuery {
    private final String eventName;

    public EventStatisticsQuery(String eventName) {
        /*
         * Intent: Identify which market study/event should be summarized.
         * Precondition: Event name must be nonblank.
         * Returns: A constructed EventStatisticsQuery instance.
         * Postcondition: Event name is trimmed and immutable.
         */
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        this.eventName = eventName.trim();
    }

    public String getEventName() {
        return eventName;
    }
}
