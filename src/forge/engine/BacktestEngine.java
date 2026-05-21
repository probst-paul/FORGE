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
import forge.execution.ExecutionEngine;
import forge.execution.FacadeForgeExecution;
import forge.execution.Fill;
import forge.execution.OrderRequest;
import forge.model.FuturesInstrumentSpec;
import forge.model.FuturesInstrumentSpecProvider;
import forge.model.StaticFuturesInstrumentSpecProvider;
import forge.reporting.BacktestResult;
import forge.reporting.ContractBacktestResult;
import forge.reporting.InstrumentBacktestResult;
import forge.strategy.StrategyCatalog;
import forge.strategy.TradingStrategy;
import forge.trade.FacadeForgeTrade;
import forge.trade.TradeLifecycleEngine;
import forge.trade.TradePlan;

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
                FacadeForgeExecution.getTheInstance().forgeExecutionAccess().createSimpleExecutionEngine()
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
                FacadeForgeExecution.getTheInstance().forgeExecutionAccess().createSimpleExecutionEngine()
        );
    }

    BacktestEngine(
            TradeBatchReaderFactory tradeBatchReaderFactory,
            StrategyCatalog strategyCatalog,
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider,
            ExecutionEngine executionEngine
    ) {
        this.tradeBatchReaderFactory = Objects.requireNonNull(tradeBatchReaderFactory, "tradeBatchReaderFactory is required");
        this.strategyCatalog = Objects.requireNonNull(strategyCatalog, "strategyCatalog is required");
        this.contractNameResolver = Objects.requireNonNull(contractNameResolver, "contractNameResolver is required");
        this.futuresInstrumentSpecProvider = Objects.requireNonNull(futuresInstrumentSpecProvider, "futuresInstrumentSpecProvider is required");
        this.executionEngine = Objects.requireNonNull(executionEngine, "executionEngine is required");
    }

    public BacktestResult run(BacktestRequest request) {
        return run(request, BacktestProgressListener.NO_OP);
    }

    public BacktestResult run(BacktestRequest request, BacktestProgressListener progressListener) {
        Objects.requireNonNull(request, "request is required");
        BacktestProgressListener listener = progressListener == null ? BacktestProgressListener.NO_OP : progressListener;
        TradingStrategy strategy = createStrategy(request.getStrategyOptions().getStrategyName());
        strategy.onBacktestStart();
        TradeBatchReader reader = tradeBatchReaderFactory.openReader(request.getContractWindows(), DEFAULT_BATCH_SIZE);
        long totalTicks = tradeBatchReaderFactory.countTicks(request.getContractWindows());
        long processedTicks = 0;
        listener.onProgress(new BacktestProgress(0, totalTicks));
        Map<String, FuturesInstrumentSpec> specsByInstrument = new HashMap<>();
        Map<String, ContractRunAccumulator> contractAccumulators = initializeContractAccumulators(request);
        Map<String, TradeLifecycleEngine> lifecycleEngines = initializeLifecycleEngines(request);

        while (true) {
            List<TradeTick> batch = reader.readNextBatch();
            if (batch.isEmpty()) {
                break;
            }
            for (TradeTick tick : batch) {
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
                MarketContext marketContext = toMarketContext(tick, specsByInstrument, lifecycleEngine.hasOpenPosition());
                accumulator.incrementTicksProcessed();
                Optional<OrderRequest> orderRequest = strategy.evaluate(marketContext);
                if (orderRequest.isPresent()) {
                    accumulator.incrementOrderSignalsGenerated();
                    Optional<TradePlan> tradePlan = strategy.getLastTradePlan();
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
            processedTicks += batch.size();
            listener.onProgress(new BacktestProgress(Math.min(processedTicks, totalTicks), totalTicks));
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

    private static class ContractRunAccumulator {
        private final String instrumentSymbol;
        private final String contractSymbol;
        private long ticksProcessed;
        private long orderSignalsGenerated;
        private final List<forge.backtest.TradeResult> trades = new ArrayList<>();

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

        private void addTrade(forge.backtest.TradeResult trade) {
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
