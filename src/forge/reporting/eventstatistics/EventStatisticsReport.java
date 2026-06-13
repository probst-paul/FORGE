package forge.reporting.eventstatistics;

import forge.engine.eventstatistics.EventStatisticsDetail;
import forge.engine.eventstatistics.EventStatisticsResult;
import forge.util.ImmutableLists;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class EventStatisticsReport implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String eventName;
    private final List<EventStatisticsResult> instrumentResults;
    private final List<EventStatisticsResult> contractResults;
    private final List<EventStatisticsDetail> eventDetails;

    public EventStatisticsReport(
            String eventName,
            List<EventStatisticsResult> instrumentResults,
            List<EventStatisticsResult> contractResults
    ) {
        this(eventName, instrumentResults, contractResults, List.of());
    }

    public EventStatisticsReport(
            String eventName,
            List<EventStatisticsResult> instrumentResults,
            List<EventStatisticsResult> contractResults,
            List<EventStatisticsDetail> eventDetails
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
        this.eventDetails = ImmutableLists.copyOfRequired(eventDetails, "eventDetails");
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

    public List<EventStatisticsDetail> getEventDetails() {
        return eventDetails;
    }
}
