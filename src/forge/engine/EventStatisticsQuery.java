package forge.engine;

public class EventStatisticsQuery {
    private final String eventName;

    public EventStatisticsQuery(String eventName) {
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        this.eventName = eventName.trim();
    }

    public String getEventName() {
        return eventName;
    }
}
