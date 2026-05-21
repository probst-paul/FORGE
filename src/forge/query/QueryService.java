package forge.query;

import forge.data.contract.ContractNameResolver;
import forge.event.EventSide;
import forge.event.FirstHourBreachEvent;
import forge.event.MarketEvent;
import forge.feature.SessionRangeFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryService {
    private final ContractNameResolver contractNameResolver;

    public QueryService() {
        this(new ContractNameResolver());
    }

    public QueryService(ContractNameResolver contractNameResolver) {
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        this.contractNameResolver = contractNameResolver;
    }

    public List<String> getSupportedQueryEventNames() {
        return List.of(FirstHourBreachEvent.EVENT_NAME);
    }

    public EventStatisticsReport summarizeEventStatistics(
            EventStatisticsQuery query,
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<MarketEvent> events
    ) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (!FirstHourBreachEvent.EVENT_NAME.equals(query.getEventName())) {
            throw new IllegalArgumentException("Unsupported event statistics query: " + query.getEventName());
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

        for (MarketEvent event : events) {
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

        private void include(EventSide side) {
            if (side == EventSide.LONG) {
                longEventCount++;
            } else if (side == EventSide.SHORT) {
                shortEventCount++;
            }
        }
    }
}
