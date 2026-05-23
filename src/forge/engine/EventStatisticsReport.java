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
