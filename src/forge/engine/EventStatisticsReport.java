package forge.engine;

import forge.util.ImmutableLists;

import java.util.List;

public class EventStatisticsReport {
    private final String eventName;
    private final List<EventStatisticsResult> instrumentResults;
    private final List<EventStatisticsResult> contractResults;

    public EventStatisticsReport(
            String eventName,
            List<EventStatisticsResult> instrumentResults,
            List<EventStatisticsResult> contractResults
    ) {
        /*
         * Intent: Store event-statistics results at instrument and contract scopes.
         * Precondition: Event name must be nonblank and result lists must be non-null with no null elements.
         * Returns: A constructed EventStatisticsReport instance.
         * Postcondition: Result lists are immutable defensive copies.
         */
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        this.eventName = eventName.trim();
        this.instrumentResults = ImmutableLists.copyOfRequired(instrumentResults, "instrumentResults");
        this.contractResults = ImmutableLists.copyOfRequired(contractResults, "contractResults");
    }

    public String getEventName() {
        return eventName;
    }

    public List<EventStatisticsResult> getInstrumentResults() {
        return instrumentResults;
    }

    public List<EventStatisticsResult> getContractResults() {
        return contractResults;
    }

}
