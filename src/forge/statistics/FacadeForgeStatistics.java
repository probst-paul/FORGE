package forge.statistics;

import forge.condition.MarketConditionOccurrence;
import forge.feature.SessionRangeFeature;
import forge.engine.EventStatisticsQuery;
import forge.engine.EventStatisticsReport;
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
        public EventStatisticsReport summarizeStudyOccurrences(
                MarketStudy study,
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<MarketConditionOccurrence> events
        ) {
            return statisticsService.summarizeStudyOccurrences(study, sessionRangeFeatures, events);
        }

        public EventStatisticsReport summarizeEventStatistics(
                EventStatisticsQuery query,
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<MarketConditionOccurrence> events
        ) {
            return statisticsService.summarizeEventStatistics(query, sessionRangeFeatures, events);
        }
    }
}
