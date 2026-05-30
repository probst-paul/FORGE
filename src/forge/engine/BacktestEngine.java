package forge.engine;

import forge.app.BacktestProgress;
import forge.app.BacktestProgressListener;
import forge.config.BacktestRequest;
import forge.data.FacadeForgeData;
import forge.data.contract.ContractNameResolver;
import forge.data.market.ContractTradeWindow;
import forge.data.market.TickDataProvider;
import forge.data.market.TradeBatchReader;
import forge.data.market.TradeTick;
import forge.data.market.TradeTickStreamProcessor;
import forge.condition.ConditionBuildService;
import forge.condition.FirstHourBreachCondition;
import forge.condition.MarketConditionOccurrence;
import forge.trade.ExecutionEngine;
import forge.trade.FacadeForgeTrade;
import forge.trade.Fill;
import forge.trade.OrderRequest;
import forge.feature.FeatureBuildService;
import forge.feature.SessionRangeFeature;
import forge.feature.TpoPeriodClassifier;
import forge.feature.TradingDayClassifier;
import forge.feature.TradingDayContext;
import forge.model.FuturesInstrumentSpec;
import forge.model.FuturesInstrumentSpecProvider;
import forge.model.StaticFuturesInstrumentSpecProvider;
import forge.reporting.BacktestResult;
import forge.reporting.ContractBacktestResult;
import forge.reporting.InstrumentBacktestResult;
import forge.strategy.StrategyCatalog;
import forge.strategy.StrategyContext;
import forge.strategy.StrategyDecision;
import forge.strategy.StrategyRequirements;
import forge.strategy.TradingStrategy;
import forge.trade.TradeLifecycleEngine;
import forge.trade.TradePlan;
import forge.trade.TradeResult;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BacktestEngine {
    private static final int DEFAULT_BATCH_SIZE = 100_000;

    private final TradeBatchReaderFactory tradeBatchReaderFactory;
    private final StrategyCatalog strategyCatalog;
    private final ContractNameResolver contractNameResolver;
    private final FuturesInstrumentSpecProvider futuresInstrumentSpecProvider;
    private final ExecutionEngine executionEngine;
    private final FeatureBuildService featureBuildService;
    private final ConditionBuildService eventBuildService;
    private final TradingDayClassifier tradingDayClassifier;
    private final TpoPeriodClassifier tpoPeriodClassifier;

    public BacktestEngine() {
        /*
         * Intent: Create the default backtest engine using the shared data and trade facades.
         * Precondition: Default facades and environment-backed data services must be available.
         * Returns: A constructed BacktestEngine instance.
         * Postcondition: Engine can stream ticks from configured data storage and simulate trades.
         */
        this(
                new TradeBatchReaderFactory() {
                    @Override
                    public TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize) {
                        return FacadeForgeData.getTheInstance()
                                .forgeDataAccess()
                                .openTradeBatchReader(windows, batchSize);
                    }

                    @Override
                    public long countTicks(List<ContractTradeWindow> windows) {
                        return FacadeForgeData.getTheInstance()
                                .forgeDataAccess()
                                .countTradeTicks(windows);
                    }
                },
                new StrategyCatalog(),
                new ContractNameResolver(),
                new StaticFuturesInstrumentSpecProvider(),
                FacadeForgeTrade.getTheInstance().forgeTradeAccess().createSimpleExecutionEngine(),
                new FeatureBuildService(),
                new ConditionBuildService(),
                new TradingDayClassifier(),
                new TpoPeriodClassifier()
        );
    }

    public BacktestEngine(TickDataProvider tickDataProvider) {
        /*
         * Intent: Create a backtest engine from a supplied tick data provider.
         * Precondition: Tick provider must be non-null.
         * Returns: A constructed BacktestEngine instance.
         * Postcondition: Engine reads backtest ticks through the supplied provider.
         */
        this(
                new TradeBatchReaderFactory() {
                    private final TickDataProvider provider = Objects.requireNonNull(tickDataProvider, "tickDataProvider is required");

                    @Override
                    public TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize) {
                        return provider.openReader(windows, batchSize);
                    }

                    @Override
                    public long countTicks(List<ContractTradeWindow> windows) {
                        return provider.countTicks(windows);
                    }
                },
                new StrategyCatalog(),
                new ContractNameResolver(),
                new StaticFuturesInstrumentSpecProvider(),
                FacadeForgeTrade.getTheInstance().forgeTradeAccess().createSimpleExecutionEngine(),
                new FeatureBuildService(),
                new ConditionBuildService(),
                new TradingDayClassifier(),
                new TpoPeriodClassifier()
        );
    }

    BacktestEngine(
            TradeBatchReaderFactory tradeBatchReaderFactory,
            StrategyCatalog strategyCatalog,
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider,
            ExecutionEngine executionEngine
    ) {
        this(
                tradeBatchReaderFactory,
                strategyCatalog,
                contractNameResolver,
                futuresInstrumentSpecProvider,
                executionEngine,
                new FeatureBuildService(),
                new ConditionBuildService(),
                new TradingDayClassifier(),
                new TpoPeriodClassifier()
        );
    }

    BacktestEngine(
            TradeBatchReaderFactory tradeBatchReaderFactory,
            StrategyCatalog strategyCatalog,
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider,
            ExecutionEngine executionEngine,
            FeatureBuildService featureBuildService,
            ConditionBuildService eventBuildService,
            TradingDayClassifier tradingDayClassifier,
            TpoPeriodClassifier tpoPeriodClassifier
    ) {
        /*
         * Intent: Create a fully wired backtest engine with explicit testable dependencies.
         * Precondition: All collaborators must be non-null and satisfy their package contracts.
         * Returns: A constructed BacktestEngine instance.
         * Postcondition: Engine orchestration delegates strategy, data, execution, feature, and condition work to supplied collaborators.
         */
        this.tradeBatchReaderFactory = Objects.requireNonNull(tradeBatchReaderFactory, "tradeBatchReaderFactory is required");
        this.strategyCatalog = Objects.requireNonNull(strategyCatalog, "strategyCatalog is required");
        this.contractNameResolver = Objects.requireNonNull(contractNameResolver, "contractNameResolver is required");
        this.futuresInstrumentSpecProvider = Objects.requireNonNull(futuresInstrumentSpecProvider, "futuresInstrumentSpecProvider is required");
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine is required");
        this.featureBuildService = Objects.requireNonNull(featureBuildService, "featureBuildService is required");
        this.eventBuildService = Objects.requireNonNull(eventBuildService, "eventBuildService is required");
        this.tradingDayClassifier = Objects.requireNonNull(tradingDayClassifier, "tradingDayClassifier is required");
        this.tpoPeriodClassifier = Objects.requireNonNull(tpoPeriodClassifier, "tpoPeriodClassifier is required");
    }

    public BacktestResult run(BacktestRequest request) {
        /*
         * Intent: Run a backtest without reporting progress.
         * Precondition: Request must be valid.
         * Returns: BacktestResult for the selected strategy and contract windows.
         * Postcondition: Backtest work is delegated to the progress-aware run method.
         */
        return run(request, BacktestProgressListener.NO_OP);
    }

    public BacktestResult run(BacktestRequest request, BacktestProgressListener progressListener) {
        /*
         * Intent: Run the selected strategy over streamed ticks, using cached or live-built derived data as needed.
         * Precondition: Request must identify a known strategy and valid contract windows.
         * Returns: BacktestResult grouped by instrument and contract.
         * Postcondition: Open positions are closed at end of data and progress is reported across all stream passes.
         */
        Objects.requireNonNull(request, "request is required");
        BacktestProgressListener listener = progressListener == null ? BacktestProgressListener.NO_OP : progressListener;
        TradingStrategy strategy = createStrategy(request.getStrategyOptions().getStrategyName());
        StrategyRequirements requirements = strategy.getRequirements();
        strategy.onBacktestStart();
        long totalTicks = tradeBatchReaderFactory.countTicks(request.getContractWindows());
        int streamPasses = streamPasses(requirements, request);
        long totalProgressTicks = totalTicks * streamPasses;
        long processedProgressTicks = 0;
        listener.onProgress(new BacktestProgress(0, totalProgressTicks));

        List<SessionRangeFeature> sessionRangeFeatures = loadOrBuildSessionRangeFeatures(
                request,
                requirements,
                listener,
                processedProgressTicks,
                totalProgressTicks
        );
        if (shouldStreamSessionRangeFeatures(request, requirements)) {
            processedProgressTicks += totalTicks;
        }

        List<MarketConditionOccurrence> marketEvents = loadOrBuildMarketConditionOccurrences(
                request,
                requirements,
                sessionRangeFeatures,
                listener,
                processedProgressTicks,
                totalProgressTicks
        );
        if (shouldStreamMarketConditionOccurrences(request, requirements)) {
            processedProgressTicks += totalTicks;
        }

        Map<SessionKey, SessionRangeFeature> featuresBySession = indexFeatures(sessionRangeFeatures);
        Map<SessionKey, List<MarketConditionOccurrence>> eventsBySession = indexEvents(marketEvents);
        Map<String, FuturesInstrumentSpec> specsByInstrument = new HashMap<>();
        Map<String, ContractRunAccumulator> contractAccumulators = initializeContractAccumulators(request);
        Map<String, TradeLifecycleEngine> lifecycleEngines = initializeLifecycleEngines(request);

        streamTicks(
                request,
                listener,
                processedProgressTicks,
                totalProgressTicks,
                new BacktestRunProcessor(
                        strategy,
                        requirements,
                        featuresBySession,
                        eventsBySession,
                        specsByInstrument,
                        contractAccumulators,
                        lifecycleEngines
                )
        );

        for (Map.Entry<String, TradeLifecycleEngine> entry : lifecycleEngines.entrySet()) {
            ContractRunAccumulator accumulator = contractAccumulators.get(entry.getKey());
            if (accumulator != null) {
                entry.getValue().closeOpenPositionAtEnd().ifPresent(accumulator::addTrade);
            }
        }

        return new BacktestResult(
                request.getStrategyOptions().getStrategyName(),
                toInstrumentResults(contractAccumulators)
        );
    }

    private List<SessionRangeFeature> loadOrBuildSessionRangeFeatures(
            BacktestRequest request,
            StrategyRequirements requirements,
            BacktestProgressListener listener,
            long processedBeforePass,
            long totalProgressTicks
    ) {
        /*
         * Intent: Provide required session range features from cache when possible, otherwise build them from ticks.
         * Precondition: Request and requirements must be valid.
         * Returns: Session range features needed by the strategy/events, or an empty list when not required.
         * Postcondition: Missing features may be built in memory for this run but are not persisted here.
         */
        if (requirements.getRequiredFeatureNames().isEmpty() && requirements.getRequiredEventNames().isEmpty()) {
            return List.of();
        }
        if (!requiresSessionRangeFeatures(requirements)) {
            return List.of();
        }
        if (areSessionRangesBuilt(request)) {
            return FacadeForgeData.getTheInstance().forgeDataAccess().loadSessionRanges(request.getContractWindows());
        }
        forge.feature.SessionRangeFeatureCalculator.Accumulator accumulator = featureBuildService.newSessionRangeAccumulator();
        streamTicks(request, listener, processedBeforePass, totalProgressTicks, accumulator);
        return accumulator.getFeatures();
    }

    private List<MarketConditionOccurrence> loadOrBuildMarketConditionOccurrences(
            BacktestRequest request,
            StrategyRequirements requirements,
            List<SessionRangeFeature> sessionRangeFeatures,
            BacktestProgressListener listener,
            long processedBeforePass,
            long totalProgressTicks
    ) {
        /*
         * Intent: Provide required market condition occurrences from cache when possible, otherwise detect them from ticks.
         * Precondition: Session range features must exist when first-hour breach detection is required.
         * Returns: Market condition occurrences required by the strategy, or an empty list when not required.
         * Postcondition: Missing occurrences may be built in memory for this run but are not persisted here.
         */
        if (!requirements.requiresEvent(FirstHourBreachCondition.EVENT_NAME)) {
            return List.of();
        }
        if (areMarketConditionOccurrencesBuilt(request)) {
            return FacadeForgeData.getTheInstance()
                    .forgeDataAccess()
                    .loadMarketConditionOccurrences(request.getContractWindows(), FirstHourBreachCondition.EVENT_NAME);
        }
        forge.condition.FirstHourBreachConditionDetector.Accumulator accumulator =
                eventBuildService.newFirstHourBreachAccumulator(sessionRangeFeatures);
        streamTicks(request, listener, processedBeforePass, totalProgressTicks, accumulator);
        return accumulator.getEvents();
    }

    private int streamPasses(StrategyRequirements requirements, BacktestRequest request) {
        /*
         * Intent: Calculate how many tick-stream passes the full backtest run will require.
         * Precondition: Requirements and request must be valid.
         * Returns: One pass for strategy execution plus extra passes for uncached derived data.
         * Postcondition: No data is read by this planning method.
         */
        int passes = 1;
        if (shouldStreamSessionRangeFeatures(request, requirements)) {
            passes++;
        }
        if (shouldStreamMarketConditionOccurrences(request, requirements)) {
            passes++;
        }
        return passes;
    }

    private boolean shouldStreamSessionRangeFeatures(BacktestRequest request, StrategyRequirements requirements) {
        return requiresSessionRangeFeatures(requirements)
                && !areSessionRangesBuilt(request);
    }

    private boolean shouldStreamMarketConditionOccurrences(BacktestRequest request, StrategyRequirements requirements) {
        return requirements.requiresEvent(FirstHourBreachCondition.EVENT_NAME)
                && !areMarketConditionOccurrencesBuilt(request);
    }

    private boolean areSessionRangesBuilt(BacktestRequest request) {
        /*
         * Intent: Safely check whether required session range features are already cached.
         * Precondition: Request must contain selected contract windows.
         * Returns: True when cached data is available, false when missing or unavailable.
         * Postcondition: Data-access failures are treated as cache misses for backtest resilience.
         */
        try {
            return FacadeForgeData.getTheInstance().forgeDataAccess().areSessionRangesBuilt(request.getContractWindows());
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    private boolean areMarketConditionOccurrencesBuilt(BacktestRequest request) {
        /*
         * Intent: Safely check whether first-hour breach occurrences are already cached.
         * Precondition: Request must contain selected contract windows.
         * Returns: True when cached event data is available, false when missing or unavailable.
         * Postcondition: Data-access failures are treated as cache misses for backtest resilience.
         */
        try {
            return FacadeForgeData.getTheInstance()
                    .forgeDataAccess()
                    .areMarketConditionOccurrencesBuilt(request.getContractWindows(), FirstHourBreachCondition.EVENT_NAME);
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    private boolean requiresSessionRangeFeatures(StrategyRequirements requirements) {
        return requirements.requiresFeature(SessionRangeFeature.FEATURE_NAME)
                || requirements.requiresEvent(FirstHourBreachCondition.EVENT_NAME);
    }

    private long streamTicks(
            BacktestRequest request,
            BacktestProgressListener listener,
            long processedBeforePass,
            long totalProgressTicks,
            TradeTickStreamProcessor processor
    ) {
        /*
         * Intent: Stream selected contract ticks through a processor while updating progress.
         * Precondition: Processor must be ready to consume ordered batches from the selected windows.
         * Returns: Number of ticks processed during this pass.
         * Postcondition: Processor receives onComplete after the final batch.
         */
        long processedTicks = 0;
        TradeBatchReader reader = tradeBatchReaderFactory.openReader(request.getContractWindows(), DEFAULT_BATCH_SIZE);
        while (true) {
            List<TradeTick> batch = reader.readNextBatch();
            if (batch.isEmpty()) {
                processor.onComplete();
                return processedTicks;
            }
            for (TradeTick tick : batch) {
                processor.onTick(tick);
            }
            processedTicks += batch.size();
            listener.onProgress(new BacktestProgress(
                    Math.min(processedBeforePass + processedTicks, totalProgressTicks),
                    totalProgressTicks
            ));
        }
    }

    private Map<SessionKey, SessionRangeFeature> indexFeatures(List<SessionRangeFeature> features) {
        /*
         * Intent: Build quick lookup for session features by contract and trading day.
         * Precondition: Feature list must be non-null.
         * Returns: Map keyed by SessionKey.
         * Postcondition: Source feature list is unchanged.
         */
        Map<SessionKey, SessionRangeFeature> featuresBySession = new HashMap<>();
        for (SessionRangeFeature feature : features) {
            featuresBySession.put(new SessionKey(feature.getContractSymbol(), feature.getSessionDate()), feature);
        }
        return featuresBySession;
    }

    private Map<SessionKey, List<MarketConditionOccurrence>> indexEvents(List<MarketConditionOccurrence> events) {
        /*
         * Intent: Build quick lookup for market condition occurrences by contract and trading day.
         * Precondition: Event list must be non-null.
         * Returns: Map keyed by SessionKey with one or more events per session.
         * Postcondition: Source event list is unchanged.
         */
        Map<SessionKey, List<MarketConditionOccurrence>> eventsBySession = new HashMap<>();
        for (MarketConditionOccurrence event : events) {
            eventsBySession
                    .computeIfAbsent(new SessionKey(event.getContractSymbol(), event.getSessionDate()), ignored -> new ArrayList<>())
                    .add(event);
        }
        return eventsBySession;
    }

    private List<MarketConditionOccurrence> eventsForTick(TradeTick tick, List<MarketConditionOccurrence> sessionEvents) {
        /*
         * Intent: Select cached session events that occur exactly on the current tick.
         * Precondition: Tick must be non-null; session events may be null or empty.
         * Returns: Matching events for the tick timestamp and price.
         * Postcondition: Source event list is unchanged.
         */
        if (sessionEvents == null || sessionEvents.isEmpty()) {
            return List.of();
        }
        List<MarketConditionOccurrence> events = new ArrayList<>();
        for (MarketConditionOccurrence event : sessionEvents) {
            if (event.getEventTime().equals(tick.getTradeDateTime())
                    && event.getEventPriceTicks() == tick.getPriceTicks()) {
                events.add(event);
            }
        }
        return events;
    }

    private Map<String, ContractRunAccumulator> initializeContractAccumulators(BacktestRequest request) {
        /*
         * Intent: Create one result accumulator per selected contract before streaming begins.
         * Precondition: Request must contain selected contract windows.
         * Returns: Map keyed by contract symbol.
         * Postcondition: Accumulators start with zero ticks, signals, and trades.
         */
        Map<String, ContractRunAccumulator> accumulators = new LinkedHashMap<>();
        for (ContractTradeWindow window : request.getContractWindows()) {
            String contractSymbol = window.getContractSymbol();
            accumulators.putIfAbsent(
                    contractSymbol,
                    new ContractRunAccumulator(
                            contractNameResolver.resolveInstrumentSymbol(contractSymbol),
                            contractSymbol
                    )
            );
        }
        return accumulators;
    }

    private Map<String, TradeLifecycleEngine> initializeLifecycleEngines(BacktestRequest request) {
        /*
         * Intent: Create one trade lifecycle engine per selected contract.
         * Precondition: Request must contain selected contract windows.
         * Returns: Map keyed by contract symbol.
         * Postcondition: Each selected contract has independent position lifecycle state.
         */
        Map<String, TradeLifecycleEngine> lifecycleEngines = new LinkedHashMap<>();
        for (ContractTradeWindow window : request.getContractWindows()) {
            lifecycleEngines.putIfAbsent(window.getContractSymbol(), createTradeLifecycleEngine());
        }
        return lifecycleEngines;
    }

    private TradeLifecycleEngine createTradeLifecycleEngine() {
        return FacadeForgeTrade.getTheInstance()
                .forgeTradeAccess()
                .createTradeLifecycleEngine();
    }

    private List<InstrumentBacktestResult> toInstrumentResults(Map<String, ContractRunAccumulator> contractAccumulators) {
        /*
         * Intent: Group per-contract accumulators into instrument-level report objects.
         * Precondition: Accumulators must be keyed by contract symbol and include instrument symbols.
         * Returns: InstrumentBacktestResult list with contract children.
         * Postcondition: Accumulators are read but not modified.
         */
        Map<String, List<ContractBacktestResult>> contractsByInstrument = new LinkedHashMap<>();
        for (ContractRunAccumulator accumulator : contractAccumulators.values()) {
            contractsByInstrument
                    .computeIfAbsent(accumulator.getInstrumentSymbol(), key -> new ArrayList<>())
                    .add(accumulator.toContractResult());
        }

        List<InstrumentBacktestResult> instrumentResults = new ArrayList<>();
        for (Map.Entry<String, List<ContractBacktestResult>> entry : contractsByInstrument.entrySet()) {
            instrumentResults.add(new InstrumentBacktestResult(entry.getKey(), entry.getValue()));
        }
        return instrumentResults;
    }

    private MarketContext toMarketContext(
            TradeTick tick,
            Map<String, FuturesInstrumentSpec> specsByInstrument,
            boolean hasOpenPosition
    ) {
        /*
         * Intent: Convert a trade tick plus instrument metadata into the strategy-facing market context.
         * Precondition: Tick must be strategy-usable and specs cache must be mutable.
         * Returns: MarketContext using tick-space price and instrument tick metadata.
         * Postcondition: Instrument spec may be cached for future ticks.
         */
        String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(tick.getContractSymbol());
        FuturesInstrumentSpec spec = specFor(tick, specsByInstrument);
        LocalDateTime timestamp = LocalDateTime.ofInstant(tick.getTradeDateTime(), ZoneOffset.UTC);
        return new MarketContext(
                instrumentSymbol,
                timestamp,
                tick.getPriceTicks(),
                spec.getTickSize(),
                spec.getTickDollarAmount(),
                hasOpenPosition
        );
    }

    private FuturesInstrumentSpec specFor(
            TradeTick tick,
            Map<String, FuturesInstrumentSpec> specsByInstrument
    ) {
        String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(tick.getContractSymbol());
        return specsByInstrument.computeIfAbsent(
                instrumentSymbol,
                futuresInstrumentSpecProvider::getBySymbol
        );
    }

    private TradingStrategy createStrategy(String strategyName) {
        /*
         * Intent: Resolve and instantiate the strategy selected in the backtest request.
         * Precondition: Strategy name must match a catalog display name.
         * Returns: New TradingStrategy instance.
         * Postcondition: Unknown names fail before ticks are read.
         */
        for (Class<? extends TradingStrategy> strategyClass : strategyCatalog.findAvailableStrategies()) {
            if (strategyCatalog.getDisplayName(strategyClass).equals(strategyName)) {
                return instantiate(strategyClass);
            }
        }
        throw new IllegalArgumentException("Unknown strategy: " + strategyName);
    }

    private TradingStrategy instantiate(Class<? extends TradingStrategy> strategyClass) {
        try {
            return strategyClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to create strategy " + strategyClass.getSimpleName(), exception);
        }
    }

    private class BacktestRunProcessor implements TradeTickStreamProcessor {
        private final TradingStrategy strategy;
        private final StrategyRequirements requirements;
        private final Map<SessionKey, SessionRangeFeature> featuresBySession;
        private final Map<SessionKey, List<MarketConditionOccurrence>> eventsBySession;
        private final Map<String, FuturesInstrumentSpec> specsByInstrument;
        private final Map<String, ContractRunAccumulator> contractAccumulators;
        private final Map<String, TradeLifecycleEngine> lifecycleEngines;

        private BacktestRunProcessor(
                TradingStrategy strategy,
                StrategyRequirements requirements,
                Map<SessionKey, SessionRangeFeature> featuresBySession,
                Map<SessionKey, List<MarketConditionOccurrence>> eventsBySession,
                Map<String, FuturesInstrumentSpec> specsByInstrument,
                Map<String, ContractRunAccumulator> contractAccumulators,
                Map<String, TradeLifecycleEngine> lifecycleEngines
        ) {
            /*
             * Intent: Create the streaming processor that evaluates strategy logic and trade lifecycle per tick.
             * Precondition: Strategy, requirements, lookup maps, accumulators, and lifecycle engines must be initialized.
             * Returns: A constructed BacktestRunProcessor instance.
             * Postcondition: Processor is ready to consume ordered TradeTick values.
             */
            this.strategy = strategy;
            this.requirements = requirements;
            this.featuresBySession = featuresBySession;
            this.eventsBySession = eventsBySession;
            this.specsByInstrument = specsByInstrument;
            this.contractAccumulators = contractAccumulators;
            this.lifecycleEngines = lifecycleEngines;
        }

        @Override
        public void onTick(TradeTick tick) {
            /*
             * Intent: Process one tick through lifecycle updates, strategy filters, signal generation, and execution.
             * Precondition: Ticks should arrive in chronological order for each contract.
             * Returns: Nothing.
             * Postcondition: Accumulators may record processed ticks, order signals, fills, or closed trades.
             */
            if (tick == null) {
                return;
            }
            ContractRunAccumulator accumulator = contractAccumulators.computeIfAbsent(
                    tick.getContractSymbol(),
                    contractSymbol -> new ContractRunAccumulator(
                            contractNameResolver.resolveInstrumentSymbol(contractSymbol),
                            contractSymbol
                    )
            );
            TradeLifecycleEngine lifecycleEngine = lifecycleEngines.computeIfAbsent(
                    tick.getContractSymbol(),
                    ignored -> createTradeLifecycleEngine()
            );
            lifecycleEngine.onTick(tick).ifPresent(accumulator::addTrade);
            TradingDayContext tradingDayContext = tradingDayClassifier.classify(tick.getTradeDateTime());
            SessionKey sessionKey = new SessionKey(tick.getContractSymbol(), tradingDayContext.getTradingDay());
            MarketContext marketContext = toMarketContext(tick, specsByInstrument, lifecycleEngine.hasOpenPosition());
            StrategyContext strategyContext = new StrategyContext(
                    marketContext,
                    tick,
                    tradingDayContext,
                    tpoPeriodClassifier.classify(tick.getTradeDateTime()),
                    featuresBySession.get(sessionKey),
                    eventsForTick(tick, eventsBySession.get(sessionKey))
            );
            accumulator.incrementTicksProcessed();
            if (!requirements.shouldEvaluate(strategyContext)) {
                return;
            }
            StrategyDecision decision = strategy.evaluate(strategyContext);
            Optional<OrderRequest> orderRequest = decision.getOrderRequest();
            if (orderRequest.isPresent()) {
                accumulator.incrementOrderSignalsGenerated();
                Optional<TradePlan> tradePlan = decision.getTradePlan();
                if (tradePlan.isPresent() && !lifecycleEngine.hasOpenPosition()) {
                    Optional<Fill> fill = executionEngine.execute(orderRequest.get(), tick);
                    fill.ifPresent(entryFill -> lifecycleEngine.openPosition(
                            entryFill,
                            tradePlan.get(),
                            specFor(tick, specsByInstrument)
                    ));
                }
            }
        }
    }

    interface TradeBatchReaderFactory {
        TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize);

        long countTicks(List<ContractTradeWindow> windows);
    }

    private static class SessionKey {
        private final String contractSymbol;
        private final java.time.LocalDate sessionDate;

        private SessionKey(String contractSymbol, java.time.LocalDate sessionDate) {
            /*
             * Intent: Create a stable key for contract/session derived data lookup.
             * Precondition: Contract symbol and session date should be non-null.
             * Returns: A constructed SessionKey instance.
             * Postcondition: Key fields are immutable.
             */
            this.contractSymbol = contractSymbol;
            this.sessionDate = sessionDate;
        }

        @Override
        public boolean equals(Object other) {
            /*
             * Intent: Compare session keys by contract symbol and trading day.
             * Precondition: Other object may be any type.
             * Returns: True when both keys identify the same contract/session.
             * Postcondition: Neither object is modified.
             */
            if (this == other) {
                return true;
            }
            if (!(other instanceof SessionKey)) {
                return false;
            }
            SessionKey that = (SessionKey) other;
            return contractSymbol.equals(that.contractSymbol) && sessionDate.equals(that.sessionDate);
        }

        @Override
        public int hashCode() {
            /*
             * Intent: Produce a hash code consistent with SessionKey equality.
             * Precondition: Key fields must be non-null.
             * Returns: Hash code for map/set lookup.
             * Postcondition: Key state is unchanged.
             */
            int result = contractSymbol.hashCode();
            result = 31 * result + sessionDate.hashCode();
            return result;
        }
    }

    private static class ContractRunAccumulator {
        private final String instrumentSymbol;
        private final String contractSymbol;
        private long ticksProcessed;
        private long orderSignalsGenerated;
        private final List<TradeResult> trades = new ArrayList<>();

        private ContractRunAccumulator(String instrumentSymbol, String contractSymbol) {
            /*
             * Intent: Accumulate ticks, order signals, and trades for one contract run.
             * Precondition: Instrument and contract symbols should be non-null.
             * Returns: A constructed ContractRunAccumulator instance.
             * Postcondition: Counts start at zero and trade list is empty.
             */
            this.instrumentSymbol = instrumentSymbol;
            this.contractSymbol = contractSymbol;
        }

        private void incrementTicksProcessed() {
            /*
             * Intent: Count one strategy-usable tick processed for this contract.
             * Precondition: Called once per tick after context construction.
             * Returns: Nothing.
             * Postcondition: Tick count increases by one.
             */
            ticksProcessed++;
        }

        private void incrementOrderSignalsGenerated() {
            /*
             * Intent: Count one strategy decision that generated an order request.
             * Precondition: Strategy decision must contain an order request.
             * Returns: Nothing.
             * Postcondition: Order signal count increases by one.
             */
            orderSignalsGenerated++;
        }

        private void addTrade(TradeResult trade) {
            /*
             * Intent: Store a completed trade for later contract/instrument reporting.
             * Precondition: Trade result should be non-null.
             * Returns: Nothing.
             * Postcondition: Trade is appended to this contract's result list.
             */
            trades.add(trade);
        }

        private String getInstrumentSymbol() {
            return instrumentSymbol;
        }

        private ContractBacktestResult toContractResult() {
            /*
             * Intent: Convert accumulated contract state into an immutable reporting result.
             * Precondition: Accumulator may have zero or more processed ticks/trades.
             * Returns: ContractBacktestResult.
             * Postcondition: Accumulator state is unchanged.
             */
            return new ContractBacktestResult(
                    contractSymbol,
                    ticksProcessed,
                    orderSignalsGenerated,
                    trades
            );
        }
    }
}
