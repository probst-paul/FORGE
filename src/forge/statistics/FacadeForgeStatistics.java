package forge.statistics;

import forge.event.MarketEventOccurrence;
import forge.feature.SessionRangeFeature;
import forge.engine.eventstatistics.EventStatisticsQuery;
import forge.reporting.eventstatistics.EventStatisticsReport;
import forge.study.MarketStudy;

import java.util.Collection;

public class FacadeForgeStatistics {
    private static final FacadeForgeStatistics THE_INSTANCE = new FacadeForgeStatistics();

    private final StatisticsService statisticsService;
    private final ForgeStatisticsAccess access = new ForgeStatisticsAccess();

    public static FacadeForgeStatistics getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeStatistics() {
        this(new StatisticsService());
    }

    public FacadeForgeStatistics(StatisticsService statisticsService) {
        if (statisticsService == null) {
            throw new IllegalArgumentException("statisticsService is required");
        }
        this.statisticsService = statisticsService;
    }

    public ForgeStatisticsAccess forgeStatisticsAccess() {
        return access;
    }

    public class ForgeStatisticsAccess {
        /*
         * Intent: Summarize a market study through the statistics facade.
         * Precondition: Study and source collections must be valid for StatisticsService.
         * Returns: EventStatisticsReport grouped by instrument and contract.
         * Postcondition: Callers do not need direct access to StatisticsService internals.
         */
        public EventStatisticsReport summarizeStudyOccurrences(
                MarketStudy study,
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<MarketEventOccurrence> events
        ) {
            return statisticsService.summarizeStudyOccurrences(study, sessionRangeFeatures, events);
        }

        /*
         * Intent: Summarize an event query through the statistics facade.
         * Precondition: Query and source collections must be valid for StatisticsService.
         * Returns: EventStatisticsReport grouped by instrument and contract.
         * Postcondition: Callers remain decoupled from statistics aggregation implementation.
         */
        public EventStatisticsReport summarizeEventStatistics(
                EventStatisticsQuery query,
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<MarketEventOccurrence> events
        ) {
            return statisticsService.summarizeEventStatistics(query, sessionRangeFeatures, events);
        }
    }
}
