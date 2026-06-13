package forge.statistics;

import forge.data.contract.ContractNameResolver;
import forge.event.EventSide;
import forge.event.MarketEventOccurrence;
import forge.feature.SessionRangeFeature;
import forge.engine.eventstatistics.EventStatisticsQuery;
import forge.engine.eventstatistics.EventStatisticsDetail;
import forge.reporting.eventstatistics.EventStatisticsReport;
import forge.engine.eventstatistics.EventStatisticsResult;
import forge.study.MarketStudy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            Collection<MarketEventOccurrence> events
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
            Collection<MarketEventOccurrence> events
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
        Map<SessionKey, SessionRangeFeature> featuresBySession = new HashMap<>();
        List<EventStatisticsDetail> details = new ArrayList<>();

        for (SessionRangeFeature feature : sessionRangeFeatures) {
            if (feature == null) {
                continue;
            }
            String contractSymbol = feature.getContractSymbol();
            String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(contractSymbol);
            contractCounts.computeIfAbsent(contractSymbol, unused -> new EventCounts()).includeSession(feature);
            instrumentCounts.computeIfAbsent(instrumentSymbol, unused -> new EventCounts()).includeSession(feature);
            featuresBySession.put(new SessionKey(contractSymbol, feature.getSessionDate()), feature);
        }

        for (MarketEventOccurrence event : events) {
            if (event == null || !query.getEventName().equals(event.getEventName())) {
                continue;
            }
            String contractSymbol = event.getContractSymbol();
            String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(contractSymbol);
            contractCounts.computeIfAbsent(contractSymbol, unused -> new EventCounts()).include(event.getSide());
            instrumentCounts.computeIfAbsent(instrumentSymbol, unused -> new EventCounts()).include(event.getSide());
            SessionRangeFeature feature = featuresBySession.get(new SessionKey(contractSymbol, event.getSessionDate()));
            if (feature != null) {
                details.add(toDetail(event, feature));
            }
        }

        details.sort(Comparator
                .comparing(EventStatisticsDetail::getContractSymbol)
                .thenComparing(EventStatisticsDetail::getSessionDate)
                .thenComparing(EventStatisticsDetail::getEventTime));

        return new EventStatisticsReport(
                query.getEventName(),
                toResults(query.getEventName(), instrumentCounts),
                toResults(query.getEventName(), contractCounts),
                details
        );
    }

    private EventStatisticsDetail toDetail(MarketEventOccurrence event, SessionRangeFeature feature) {
        return new EventStatisticsDetail(
                event.getContractSymbol(),
                event.getSessionDate(),
                event.getEventName(),
                event.getSide(),
                event.getEventTime(),
                event.getEventPriceTicks(),
                feature.getOvernightLowTicks(),
                feature.getOvernightHighTicks(),
                feature.getFirstHourLowTicks(),
                feature.getFirstHourHighTicks(),
                feature.getRthLowTicks(),
                feature.getRthHighTicks(),
                feature.getOvernightVolume(),
                feature.getFirstHourVolume(),
                feature.getRthVolume(),
                feature.getOvernightTradeCount(),
                feature.getFirstHourTradeCount(),
                feature.getRthTradeCount()
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
                    counts.highEventCount,
                    counts.lowEventCount,
                    counts.average(counts.overnightRangeTicksTotal),
                    counts.average(counts.firstHourRangeTicksTotal),
                    counts.average(counts.rthRangeTicksTotal),
                    counts.average(counts.overnightVolumeTotal),
                    counts.average(counts.firstHourVolumeTotal),
                    counts.average(counts.rthVolumeTotal)
            ));
        }
        results.sort(Comparator.comparing(EventStatisticsResult::getScopeName));
        return results;
    }

    private static class EventCounts {
        private long sessionsAnalyzed;
        private long highEventCount;
        private long lowEventCount;
        private long overnightRangeTicksTotal;
        private long firstHourRangeTicksTotal;
        private long rthRangeTicksTotal;
        private long overnightVolumeTotal;
        private long firstHourVolumeTotal;
        private long rthVolumeTotal;

        private void includeSession(SessionRangeFeature feature) {
            sessionsAnalyzed++;
            overnightRangeTicksTotal += feature.getOvernightRangeTicks();
            firstHourRangeTicksTotal += feature.getFirstHourRangeTicks();
            rthRangeTicksTotal += feature.getRthRangeTicks();
            overnightVolumeTotal += feature.getOvernightVolume();
            firstHourVolumeTotal += feature.getFirstHourVolume();
            rthVolumeTotal += feature.getRthVolume();
        }

        private void include(EventSide side) {
            /*
             * Intent: Count a directional event occurrence.
             * Precondition: side may be null or a non-directional value.
             * Returns: Nothing.
             * Postcondition: HIGH and LOW counters are incremented when applicable.
             */
            if (side == EventSide.HIGH) {
                highEventCount++;
            } else if (side == EventSide.LOW) {
                lowEventCount++;
            }
        }

        private double average(long total) {
            if (sessionsAnalyzed == 0) {
                return 0;
            }
            return (double) total / sessionsAnalyzed;
        }
    }

    private static class SessionKey {
        private final String contractSymbol;
        private final java.time.LocalDate sessionDate;

        private SessionKey(String contractSymbol, java.time.LocalDate sessionDate) {
            this.contractSymbol = contractSymbol;
            this.sessionDate = sessionDate;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SessionKey)) {
                return false;
            }
            SessionKey that = (SessionKey) other;
            return Objects.equals(contractSymbol, that.contractSymbol)
                    && Objects.equals(sessionDate, that.sessionDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contractSymbol, sessionDate);
        }
    }
}
