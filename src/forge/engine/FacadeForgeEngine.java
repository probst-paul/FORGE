package forge.engine;

import forge.app.BacktestProgressListener;
import forge.condition.ConditionBuildService;
import forge.condition.MarketConditionOccurrence;
import forge.config.BacktestRequest;
import forge.data.FacadeForgeData;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;
import forge.reporting.BacktestResult;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

public class FacadeForgeEngine {
    private static final FacadeForgeEngine THE_INSTANCE = new FacadeForgeEngine();

    private final BacktestEngine backtestEngine;
    private final QueryService queryService;
    private final EventStatisticsQueryRunner eventStatisticsQueryRunner;
    private final ForgeEngineAccess access = new ForgeEngineAccess();

    public static FacadeForgeEngine getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeEngine() {
        this(new BacktestEngine(), new QueryService());
    }

    public FacadeForgeEngine(BacktestEngine backtestEngine) {
        this(backtestEngine, new QueryService());
    }

    public FacadeForgeEngine(BacktestEngine backtestEngine, QueryService queryService) {
        this(
                backtestEngine,
                queryService,
                new EventStatisticsQueryRunner(
                defaultTradeTickSource(),
                defaultDerivedDataStore(),
                new FeatureBuildService(),
                new ConditionBuildService(),
                queryService
                )
        );
    }

    public FacadeForgeEngine(QueryService queryService, EventStatisticsQueryRunner eventStatisticsQueryRunner) {
        this(new BacktestEngine(), queryService, eventStatisticsQueryRunner);
    }

    public FacadeForgeEngine(
            BacktestEngine backtestEngine,
            QueryService queryService,
            EventStatisticsQueryRunner eventStatisticsQueryRunner
    ) {
        if (backtestEngine == null) {
            throw new IllegalArgumentException("backtestEngine is required");
        }
        if (queryService == null) {
            throw new IllegalArgumentException("queryService is required");
        }
        if (eventStatisticsQueryRunner == null) {
            throw new IllegalArgumentException("eventStatisticsQueryRunner is required");
        }
        this.backtestEngine = backtestEngine;
        this.queryService = queryService;
        this.eventStatisticsQueryRunner = eventStatisticsQueryRunner;
    }

    public ForgeEngineAccess forgeEngineAccess() {
        return access;
    }

    public class ForgeEngineAccess {
        public BacktestEngine getBacktestEngine() {
            return backtestEngine;
        }

        public MarketContext createMarketContext(
                String instrumentSymbol,
                LocalDateTime timestamp,
                double lastPrice,
                boolean hasOpenPosition
        ) {
            return new MarketContext(instrumentSymbol, timestamp, lastPrice, hasOpenPosition);
        }

        public MarketContext createMarketContext(
                String instrumentSymbol,
                LocalDateTime timestamp,
                long lastPriceTicks,
                double tickSize,
                double tickDollarValue,
                boolean hasOpenPosition
        ) {
            return new MarketContext(
                    instrumentSymbol,
                    timestamp,
                    lastPriceTicks,
                    tickSize,
                    tickDollarValue,
                    hasOpenPosition
            );
        }

        public BacktestResult run(BacktestRequest request) {
            return backtestEngine.run(request);
        }

        public BacktestResult run(BacktestRequest request, BacktestProgressListener progressListener) {
            return backtestEngine.run(request, progressListener);
        }

        public List<String> getSupportedQueryEventNames() {
            return queryService.getSupportedQueryEventNames();
        }

        public String getEventStatisticDisplayName(String eventName) {
            return queryService.getEventStatisticDisplayName(eventName);
        }

        public String getEventStatisticDescription(String eventName) {
            return queryService.getEventStatisticDescription(eventName);
        }

        public EventStatisticsReport summarizeEventStatistics(
                EventStatisticsQuery query,
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<MarketConditionOccurrence> events
        ) {
            return queryService.summarizeEventStatistics(query, sessionRangeFeatures, events);
        }

        public EventStatisticsReport runEventStatistics(EventStatisticsQueryRequest request) {
            return eventStatisticsQueryRunner.run(request);
        }
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
            public boolean areMarketConditionOccurrencesBuilt(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .areMarketConditionOccurrencesBuilt(windows, eventName);
            }

            @Override
            public java.util.List<forge.condition.MarketConditionOccurrence> loadMarketConditionOccurrences(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .loadMarketConditionOccurrences(windows, eventName);
            }

            @Override
            public void saveMarketConditionOccurrences(
                    java.util.Collection<forge.condition.MarketConditionOccurrence> marketConditionOccurrences
            ) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .saveMarketConditionOccurrences(marketConditionOccurrences);
            }

            @Override
            public void markMarketConditionOccurrencesBuilt(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .markMarketConditionOccurrencesBuilt(windows, eventName);
            }
        };
    }
}
