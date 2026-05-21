package forge.query;

import forge.data.FacadeForgeData;
import forge.event.MarketEvent;
import forge.event.EventBuildService;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;

import java.util.Collection;
import java.util.List;

public class FacadeForgeQuery {
    private static final FacadeForgeQuery THE_INSTANCE = new FacadeForgeQuery();

    private final QueryService queryService;
    private final EventStatisticsQueryRunner eventStatisticsQueryRunner;
    private final ForgeQueryAccess access = new ForgeQueryAccess();

    public static FacadeForgeQuery getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeQuery() {
        this(
                new QueryService(),
                new EventStatisticsQueryRunner(
                        defaultTradeTickSource(),
                        defaultDerivedDataStore(),
                        new FeatureBuildService(),
                        new EventBuildService(),
                        new QueryService()
                )
        );
    }

    public FacadeForgeQuery(QueryService queryService) {
        this(
                queryService,
                new EventStatisticsQueryRunner(
                        defaultTradeTickSource(),
                        defaultDerivedDataStore(),
                        new FeatureBuildService(),
                        new EventBuildService(),
                        queryService
                )
        );
    }

    public FacadeForgeQuery(QueryService queryService, EventStatisticsQueryRunner eventStatisticsQueryRunner) {
        if (queryService == null) {
            throw new IllegalArgumentException("queryService is required");
        }
        if (eventStatisticsQueryRunner == null) {
            throw new IllegalArgumentException("eventStatisticsQueryRunner is required");
        }
        this.queryService = queryService;
        this.eventStatisticsQueryRunner = eventStatisticsQueryRunner;
    }

    public ForgeQueryAccess forgeQueryAccess() {
        return access;
    }

    private static QueryTradeTickSource defaultTradeTickSource() {
        return new QueryTradeTickSource() {
            @Override
            public forge.data.market.TradeBatchReader openTradeBatchReader(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    int batchSize
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .openTradeBatchReader(windows, batchSize);
            }

            @Override
            public long countTradeTicks(java.util.List<forge.data.market.ContractTradeWindow> windows) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .countTradeTicks(windows);
            }
        };
    }

    private static QueryDerivedDataStore defaultDerivedDataStore() {
        return new QueryDerivedDataStore() {
            @Override
            public boolean areSessionRangesBuilt(java.util.List<forge.data.market.ContractTradeWindow> windows) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .areSessionRangesBuilt(windows);
            }

            @Override
            public java.util.List<forge.feature.SessionRangeFeature> loadSessionRanges(
                    java.util.List<forge.data.market.ContractTradeWindow> windows
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .loadSessionRanges(windows);
            }

            @Override
            public void saveSessionRanges(java.util.Collection<forge.feature.SessionRangeFeature> sessionRangeFeatures) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .saveSessionRanges(sessionRangeFeatures);
            }

            @Override
            public void markSessionRangesBuilt(java.util.List<forge.data.market.ContractTradeWindow> windows) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .markSessionRangesBuilt(windows);
            }

            @Override
            public boolean areMarketEventsBuilt(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .areMarketEventsBuilt(windows, eventName);
            }

            @Override
            public java.util.List<forge.event.MarketEvent> loadMarketEvents(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .loadMarketEvents(windows, eventName);
            }

            @Override
            public void saveMarketEvents(java.util.Collection<forge.event.MarketEvent> marketEvents) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .saveMarketEvents(marketEvents);
            }

            @Override
            public void markMarketEventsBuilt(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .markMarketEventsBuilt(windows, eventName);
            }
        };
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

        public EventStatisticsReport runEventStatistics(EventStatisticsQueryRequest request) {
            return eventStatisticsQueryRunner.run(request);
        }
    }
}
