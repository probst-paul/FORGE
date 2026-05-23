package forge.engine;

import forge.condition.MarketConditionOccurrence;
import forge.feature.SessionRangeFeature;
import forge.statistics.StatisticsService;
import forge.study.MarketStudy;
import forge.study.StudyCatalog;

import java.util.Collection;
import java.util.List;

public class QueryService {
    private final StudyCatalog studyCatalog;
    private final StatisticsService statisticsService;

    public QueryService() {
        this(new StudyCatalog(), new StatisticsService());
    }

    public QueryService(StudyCatalog studyCatalog, StatisticsService statisticsService) {
        if (studyCatalog == null) {
            throw new IllegalArgumentException("studyCatalog is required");
        }
        if (statisticsService == null) {
            throw new IllegalArgumentException("statisticsService is required");
        }
        this.studyCatalog = studyCatalog;
        this.statisticsService = statisticsService;
    }

    public List<String> getSupportedQueryEventNames() {
        return studyCatalog.findAvailableStudyNames();
    }

    public String getEventStatisticDisplayName(String eventName) {
        return studyCatalog.getStudy(eventName).getDisplayName();
    }

    public String getEventStatisticDescription(String eventName) {
        return studyCatalog.getStudy(eventName).getDescription();
    }

    public EventStatisticsReport summarizeEventStatistics(
            EventStatisticsQuery query,
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<MarketConditionOccurrence> events
    ) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        MarketStudy study = studyCatalog.getStudy(query.getEventName());
        return statisticsService.summarizeStudyOccurrences(study, sessionRangeFeatures, events);
    }
}
