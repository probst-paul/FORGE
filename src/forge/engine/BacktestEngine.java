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
import forge.model.FuturesInstrumentSpec;
import forge.model.FuturesInstrumentSpecProvider;
import forge.model.StaticFuturesInstrumentSpecProvider;
import forge.reporting.BacktestResult;
import forge.reporting.ContractBacktestResult;
import forge.reporting.InstrumentBacktestResult;
import forge.strategy.StrategyCatalog;
import forge.strategy.TradingStrategy;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BacktestEngine {
    private static final int DEFAULT_BATCH_SIZE = 100_000;

    private final TradeBatchReaderFactory tradeBatchReaderFactory;
    private final StrategyCatalog strategyCatalog;
    private final ContractNameResolver contractNameResolver;
    private final FuturesInstrumentSpecProvider futuresInstrumentSpecProvider;

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
                new StaticFuturesInstrumentSpecProvider()
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
                new StaticFuturesInstrumentSpecProvider()
        );
    }

    BacktestEngine(
            TradeBatchReaderFactory tradeBatchReaderFactory,
            StrategyCatalog strategyCatalog,
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider
    ) {
        this.tradeBatchReaderFactory = Objects.requireNonNull(tradeBatchReaderFactory, "tradeBatchReaderFactory is required");
        this.strategyCatalog = Objects.requireNonNull(strategyCatalog, "strategyCatalog is required");
        this.contractNameResolver = Objects.requireNonNull(contractNameResolver, "contractNameResolver is required");
        this.futuresInstrumentSpecProvider = Objects.requireNonNull(futuresInstrumentSpecProvider, "futuresInstrumentSpecProvider is required");
    }

    public BacktestResult run(BacktestRequest request) {
        return run(request, BacktestProgressListener.NO_OP);
    }

    public BacktestResult run(BacktestRequest request, BacktestProgressListener progressListener) {
        Objects.requireNonNull(request, "request is required");
        BacktestProgressListener listener = progressListener == null ? BacktestProgressListener.NO_OP : progressListener;
        TradingStrategy strategy = createStrategy(request.getStrategyOptions().getStrategyName());
        TradeBatchReader reader = tradeBatchReaderFactory.openReader(request.getContractWindows(), DEFAULT_BATCH_SIZE);
        long totalTicks = tradeBatchReaderFactory.countTicks(request.getContractWindows());
        long processedTicks = 0;
        listener.onProgress(new BacktestProgress(0, totalTicks));
        Map<String, FuturesInstrumentSpec> specsByInstrument = new HashMap<>();
        Map<String, ContractRunAccumulator> contractAccumulators = initializeContractAccumulators(request);

        while (true) {
            List<TradeTick> batch = reader.readNextBatch();
            if (batch.isEmpty()) {
                break;
            }
            for (TradeTick tick : batch) {
                MarketContext marketContext = toMarketContext(tick, specsByInstrument);
                ContractRunAccumulator accumulator = contractAccumulators.computeIfAbsent(
                        tick.getContractSymbol(),
                        contractSymbol -> new ContractRunAccumulator(
                                contractNameResolver.resolveInstrumentSymbol(contractSymbol),
                                contractSymbol
                        )
                );
                accumulator.incrementTicksProcessed();
                if (strategy.evaluate(marketContext).isPresent()) {
                    accumulator.incrementOrderSignalsGenerated();
                }
            }
            processedTicks += batch.size();
            listener.onProgress(new BacktestProgress(Math.min(processedTicks, totalTicks), totalTicks));
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
            Map<String, FuturesInstrumentSpec> specsByInstrument
    ) {
        String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(tick.getContractSymbol());
        FuturesInstrumentSpec spec = specsByInstrument.computeIfAbsent(
                instrumentSymbol,
                futuresInstrumentSpecProvider::getBySymbol
        );
        LocalDateTime timestamp = LocalDateTime.ofInstant(tick.getTradeDateTime(), ZoneOffset.UTC);
        return new MarketContext(
                instrumentSymbol,
                timestamp,
                tick.getPriceTicks(),
                spec.getTickSize(),
                spec.getTickDollarAmount(),
                false
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

        private String getInstrumentSymbol() {
            return instrumentSymbol;
        }

        private ContractBacktestResult toContractResult() {
            return new ContractBacktestResult(
                    contractSymbol,
                    ticksProcessed,
                    orderSignalsGenerated,
                    Collections.emptyList()
            );
        }
    }
}
