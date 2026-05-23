package forge.engine;

import java.util.ArrayList;
import java.util.Collections;
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
        this.instrumentResults = immutableCopy(instrumentResults, "instrumentResults");
        this.contractResults = immutableCopy(contractResults, "contractResults");
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

    private List<EventStatisticsResult> immutableCopy(List<EventStatisticsResult> results, String name) {
        if (results == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Collections.unmodifiableList(new ArrayList<>(results));
    }
}
