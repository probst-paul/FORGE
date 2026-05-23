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
import forge.event.EventBuildService;
import forge.event.MarketEvent;
import forge.execution.ExecutionEngine;
import forge.execution.FacadeForgeExecution;
import forge.execution.Fill;
import forge.execution.OrderRequest;
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
import forge.trade.FacadeForgeTrade;
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
    private final EventBuildService eventBuildService;
    private final TradingDayClassifier tradingDayClassifier;
    private final TpoPeriodClassifier tpoPeriodClassifier;

    public BacktestEngine() {
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
                FacadeForgeExecution.getTheInstance().forgeExecutionAccess().createSimpleExecutionEngine(),
                new FeatureBuildService(),
                new EventBuildService(),
                new TradingDayClassifier(),
                new TpoPeriodClassifier()
        );
    }

    public BacktestEngine(TickDataProvider tickDataProvider) {
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
                FacadeForgeExecution.getTheInstance().forgeExecutionAccess().createSimpleExecutionEngine(),
                new FeatureBuildService(),
                new EventBuildService(),
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
                new EventBuildService(),
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
            EventBuildService eventBuildService,
            TradingDayClassifier tradingDayClassifier,
            TpoPeriodClassifier tpoPeriodClassifier
    ) {
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
        return run(request, BacktestProgressListener.NO_OP);
    }

    public BacktestResult run(BacktestRequest request, BacktestProgressListener progressListener) {
        Objects.requireNonNull(request, "request is required");
        BacktestProgressListener listener = progressListener == null ? BacktestProgressListener.NO_OP : progressListener;
        TradingStrategy strategy = createStrategy(request.getStrategyOptions().getStrategyName());
        StrategyRequirements requirements = strategy.getRequirements();
        strategy.onBacktestStart();
        long totalTicks = tradeBatchReaderFactory.countTicks(request.getContractWindows());
        listener.onProgress(new BacktestProgress(0, totalTicks));
        List<TradeTick> ticks = readTicks(request, listener, totalTicks);
        List<SessionRangeFeature> sessionRangeFeatures = buildSessionRangeFeatures(requirements, ticks);
        List<MarketEvent> marketEvents = buildMarketEvents(requirements, sessionRangeFeatures, ticks);
        Map<SessionKey, SessionRangeFeature> featuresBySession = indexFeatures(sessionRangeFeatures);
        Map<SessionKey, List<MarketEvent>> eventsBySession = indexEvents(marketEvents);
        Map<String, FuturesInstrumentSpec> specsByInstrument = new HashMap<>();
        Map<String, ContractRunAccumulator> contractAccumulators = initializeContractAccumulators(request);
        Map<String, TradeLifecycleEngine> lifecycleEngines = initializeLifecycleEngines(request);

        for (TradeTick tick : ticks) {
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
                continue;
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

    private List<SessionRangeFeature> buildSessionRangeFeatures(
            StrategyRequirements requirements,
            List<TradeTick> ticks
    ) {
        if (requirements.getRequiredFeatureNames().isEmpty() && requirements.getRequiredEventNames().isEmpty()) {
            return List.of();
        }
        if (requirements.requiresFeature(SessionRangeFeature.FEATURE_NAME)
                || requirements.requiresEvent(forge.event.FirstHourBreachEvent.EVENT_NAME)) {
            return featureBuildService.calculateSessionRanges(ticks);
        }
        return List.of();
    }

    private List<MarketEvent> buildMarketEvents(
            StrategyRequirements requirements,
            List<SessionRangeFeature> sessionRangeFeatures,
            List<TradeTick> ticks
    ) {
        if (requirements.requiresEvent(forge.event.FirstHourBreachEvent.EVENT_NAME)) {
            return eventBuildService.detectFirstHourBreachEvents(sessionRangeFeatures, ticks);
        }
        return List.of();
    }

    private List<TradeTick> readTicks(
            BacktestRequest request,
            BacktestProgressListener listener,
            long totalTicks
    ) {
        List<TradeTick> ticks = new ArrayList<>();
        long processedTicks = 0;
        TradeBatchReader reader = tradeBatchReaderFactory.openReader(request.getContractWindows(), DEFAULT_BATCH_SIZE);
        while (true) {
            List<TradeTick> batch = reader.readNextBatch();
            if (batch.isEmpty()) {
                break;
            }
            ticks.addAll(batch);
            processedTicks += batch.size();
            listener.onProgress(new BacktestProgress(Math.min(processedTicks, totalTicks), totalTicks));
        }
        return ticks;
    }

    private Map<SessionKey, SessionRangeFeature> indexFeatures(List<SessionRangeFeature> features) {
        Map<SessionKey, SessionRangeFeature> featuresBySession = new HashMap<>();
        for (SessionRangeFeature feature : features) {
            featuresBySession.put(new SessionKey(feature.getContractSymbol(), feature.getSessionDate()), feature);
        }
        return featuresBySession;
    }

    private Map<SessionKey, List<MarketEvent>> indexEvents(List<MarketEvent> events) {
        Map<SessionKey, List<MarketEvent>> eventsBySession = new HashMap<>();
        for (MarketEvent event : events) {
            eventsBySession
                    .computeIfAbsent(new SessionKey(event.getContractSymbol(), event.getSessionDate()), ignored -> new ArrayList<>())
                    .add(event);
        }
        return eventsBySession;
    }

    private List<MarketEvent> eventsForTick(TradeTick tick, List<MarketEvent> sessionEvents) {
        if (sessionEvents == null || sessionEvents.isEmpty()) {
            return List.of();
        }
        List<MarketEvent> events = new ArrayList<>();
        for (MarketEvent event : sessionEvents) {
            if (event.getEventTime().equals(tick.getTradeDateTime())
                    && event.getEventPriceTicks() == tick.getPriceTicks()) {
                events.add(event);
            }
        }
        return events;
    }

    private Map<String, ContractRunAccumulator> initializeContractAccumulators(BacktestRequest request) {
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

    interface TradeBatchReaderFactory {
        TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize);

        long countTicks(List<ContractTradeWindow> windows);
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
            return contractSymbol.equals(that.contractSymbol) && sessionDate.equals(that.sessionDate);
        }

        @Override
        public int hashCode() {
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
            this.instrumentSymbol = instrumentSymbol;
            this.contractSymbol = contractSymbol;
        }

        private void incrementTicksProcessed() {
            ticksProcessed++;
        }

        private void incrementOrderSignalsGenerated() {
            orderSignalsGenerated++;
        }

        private void addTrade(TradeResult trade) {
            trades.add(trade);
        }

        private String getInstrumentSymbol() {
            return instrumentSymbol;
        }

        private ContractBacktestResult toContractResult() {
            return new ContractBacktestResult(
                    contractSymbol,
                    ticksProcessed,
                    orderSignalsGenerated,
                    trades
            );
        }
    }
}
