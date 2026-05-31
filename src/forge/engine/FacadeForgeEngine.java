package forge.engine;

import forge.app.BacktestProgressListener;
import forge.event.EventBuildService;
import forge.event.MarketEventOccurrence;
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
        /*
         * Intent: Provide the shared engine facade used by app, CLI, and GUI layers.
         * Precondition: Static facade instance must have initialized successfully.
         * Returns: Singleton FacadeForgeEngine instance.
         * Postcondition: No new facade is created.
         */
        return THE_INSTANCE;
    }

    public FacadeForgeEngine() {
        /*
         * Intent: Create the engine facade with default backtest and query services.
         * Precondition: Default engine dependencies must be available.
         * Returns: A constructed FacadeForgeEngine instance.
         * Postcondition: Facade can run backtests and event statistics.
         */
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
                new EventBuildService(),
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
        /*
         * Intent: Create the engine facade with explicit orchestration dependencies.
         * Precondition: Backtest engine, query service, and event-statistics runner must be non-null.
         * Returns: A constructed FacadeForgeEngine instance.
         * Postcondition: Public access methods delegate to supplied dependencies.
         */
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
        /*
         * Intent: Expose the public access object for engine package operations.
         * Precondition: Facade must be constructed.
         * Returns: Stable ForgeEngineAccess instance.
         * Postcondition: Facade state is unchanged.
         */
        return access;
    }

    public class ForgeEngineAccess {
        public BacktestEngine getBacktestEngine() {
            /*
             * Intent: Expose the underlying backtest engine for callers that need direct engine access.
             * Precondition: Facade must be constructed.
             * Returns: BacktestEngine dependency.
             * Postcondition: Engine state is unchanged.
             */
            return backtestEngine;
        }

        public MarketContext createMarketContext(
                String instrumentSymbol,
                LocalDateTime timestamp,
                double lastPrice,
                boolean hasOpenPosition
        ) {
            /*
             * Intent: Create a legacy market context from floating-point price input.
             * Precondition: Instrument symbol, timestamp, and price must be valid.
             * Returns: MarketContext using default tick metadata.
             * Postcondition: No engine state is changed.
             */
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
            /*
             * Intent: Create a tick-normalized market context for strategy evaluation.
             * Precondition: Instrument symbol, timestamp, tick price, tick size, and tick value must be valid.
             * Returns: MarketContext with tick-space and display-price values.
             * Postcondition: No engine state is changed.
             */
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
            /*
             * Intent: Run a backtest without progress reporting.
             * Precondition: Backtest request must be valid.
             * Returns: BacktestResult from the backtest engine.
             * Postcondition: Engine may stream ticks and simulate trades.
             */
            return backtestEngine.run(request);
        }

        public BacktestResult run(BacktestRequest request, BacktestProgressListener progressListener) {
            /*
             * Intent: Run a backtest with progress reporting.
             * Precondition: Backtest request must be valid; progress listener may be null.
             * Returns: BacktestResult from the backtest engine.
             * Postcondition: Progress listener receives updates while ticks are processed.
             */
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
                Collection<MarketEventOccurrence> events
        ) {
            /*
             * Intent: Summarize already-loaded derived data into event statistics.
             * Precondition: Query and input collections must be valid for the query service.
             * Returns: EventStatisticsReport grouped by instrument and contract.
             * Postcondition: Input collections are not modified.
             */
            return queryService.summarizeEventStatistics(query, sessionRangeFeatures, events);
        }

        public EventStatisticsReport runEventStatistics(EventStatisticsQueryRequest request) {
            /*
             * Intent: Run the full event-statistics workflow, building/loading derived data as needed.
             * Precondition: Query request must identify supported event statistics and valid contract windows.
             * Returns: EventStatisticsReport.
             * Postcondition: Missing derived data may be persisted by the runner.
             */
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
            public boolean areMarketEventOccurrencesBuilt(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .areMarketEventOccurrencesBuilt(windows, eventName);
            }

            @Override
            public java.util.List<forge.event.MarketEventOccurrence> loadMarketEventOccurrences(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                return FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .loadMarketEventOccurrences(windows, eventName);
            }

            @Override
            public void saveMarketEventOccurrences(
                    java.util.Collection<forge.event.MarketEventOccurrence> marketConditionOccurrences
            ) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .saveMarketEventOccurrences(marketConditionOccurrences);
            }

            @Override
            public void markMarketEventOccurrencesBuilt(
                    java.util.List<forge.data.market.ContractTradeWindow> windows,
                    String eventName
            ) {
                FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .markMarketEventOccurrencesBuilt(windows, eventName);
            }
        };
    }
}
