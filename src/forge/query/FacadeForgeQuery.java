package forge.query;

import forge.event.MarketEvent;
import forge.feature.SessionRangeFeature;

import java.util.Collection;
import java.util.List;

public class FacadeForgeQuery {
    private static final FacadeForgeQuery THE_INSTANCE = new FacadeForgeQuery();

    private final QueryService queryService;
    private final ForgeQueryAccess access = new ForgeQueryAccess();

    public static FacadeForgeQuery getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeQuery() {
        this(new QueryService());
    }

    public FacadeForgeQuery(QueryService queryService) {
        if (queryService == null) {
            throw new IllegalArgumentException("queryService is required");
        }
        this.queryService = queryService;
    }

    public ForgeQueryAccess forgeQueryAccess() {
        return access;
    }

    public class ForgeQueryAccess {
        public List<String> getSupportedQueryEventNames() {
            return queryService.getSupportedQueryEventNames();
        }

        public EventStatisticsReport summarizeEventStatistics(
                EventStatisticsQuery query,
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<MarketEvent> events
        ) {
            return queryService.summarizeEventStatistics(query, sessionRangeFeatures, events);
        }
    }
}
