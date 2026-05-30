package forge.statistics;

import forge.data.contract.ContractNameResolver;
import forge.condition.ConditionSide;
import forge.condition.MarketConditionOccurrence;
import forge.feature.SessionRangeFeature;
import forge.engine.EventStatisticsQuery;
import forge.engine.EventStatisticsReport;
import forge.engine.EventStatisticsResult;
import forge.study.MarketStudy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private final ContractNameResolver contractNameResolver;

    public StatisticsService() {
        this(new ContractNameResolver());
    }

    public StatisticsService(ContractNameResolver contractNameResolver) {
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        this.contractNameResolver = contractNameResolver;
    }

    public EventStatisticsReport summarizeStudyOccurrences(
            MarketStudy study,
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<MarketConditionOccurrence> events
    ) {
        /*
         * Intent: Summarize occurrences for a named market study.
         * Precondition: study must be non-null; feature/event collections are validated by summarizeEventStatistics().
         * Returns: EventStatisticsReport grouped by instrument and contract.
         * Postcondition: Input feature and event collections are not modified.
         */
        if (study == null) {
            throw new IllegalArgumentException("study is required");
        }
        return summarizeEventStatistics(
                new EventStatisticsQuery(study.getName()),
                sessionRangeFeatures,
                events
        );
    }

    public EventStatisticsReport summarizeEventStatistics(
            EventStatisticsQuery query,
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<MarketConditionOccurrence> events
    ) {
        /*
         * Intent: Aggregate event counts by instrument and contract for one event query.
         * Precondition: query, sessionRangeFeatures, and events must be non-null.
         * Returns: EventStatisticsReport with session counts, long counts, and short counts.
         * Postcondition: Null collection entries are ignored and source collections are unchanged.
         */
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (sessionRangeFeatures == null) {
            throw new IllegalArgumentException("sessionRangeFeatures is required");
        }
        if (events == null) {
            throw new IllegalArgumentException("events is required");
        }

        Map<String, EventCounts> instrumentCounts = new HashMap<>();
        Map<String, EventCounts> contractCounts = new HashMap<>();

        for (SessionRangeFeature feature : sessionRangeFeatures) {
            if (feature == null) {
                continue;
            }
            String contractSymbol = feature.getContractSymbol();
            String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(contractSymbol);
            contractCounts.computeIfAbsent(contractSymbol, unused -> new EventCounts()).sessionsAnalyzed++;
            instrumentCounts.computeIfAbsent(instrumentSymbol, unused -> new EventCounts()).sessionsAnalyzed++;
        }

        for (MarketConditionOccurrence event : events) {
            if (event == null || !query.getEventName().equals(event.getEventName())) {
                continue;
            }
            String contractSymbol = event.getContractSymbol();
            String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(contractSymbol);
            contractCounts.computeIfAbsent(contractSymbol, unused -> new EventCounts()).include(event.getSide());
            instrumentCounts.computeIfAbsent(instrumentSymbol, unused -> new EventCounts()).include(event.getSide());
        }

        return new EventStatisticsReport(
                query.getEventName(),
                toResults(query.getEventName(), instrumentCounts),
                toResults(query.getEventName(), contractCounts)
        );
    }

    private List<EventStatisticsResult> toResults(String eventName, Map<String, EventCounts> countsByScope) {
        /*
         * Intent: Convert mutable count buckets into sorted report results.
         * Precondition: eventName and countsByScope must represent one completed aggregation.
         * Returns: Results sorted by scope name.
         * Postcondition: Count buckets are read but not modified.
         */
        List<EventStatisticsResult> results = new ArrayList<>();
        for (Map.Entry<String, EventCounts> entry : countsByScope.entrySet()) {
            EventCounts counts = entry.getValue();
            results.add(new EventStatisticsResult(
                    entry.getKey(),
                    eventName,
                    counts.sessionsAnalyzed,
                    counts.longEventCount,
                    counts.shortEventCount
            ));
        }
        results.sort(Comparator.comparing(EventStatisticsResult::getScopeName));
        return results;
    }

    private static class EventCounts {
        private long sessionsAnalyzed;
        private long longEventCount;
        private long shortEventCount;

        private void include(ConditionSide side) {
            /*
             * Intent: Count a directional event occurrence.
             * Precondition: side may be null or a non-directional value.
             * Returns: Nothing.
             * Postcondition: LONG and SHORT counters are incremented when applicable.
             */
            if (side == ConditionSide.LONG) {
                longEventCount++;
            } else if (side == ConditionSide.SHORT) {
                shortEventCount++;
            }
        }
    }
}
