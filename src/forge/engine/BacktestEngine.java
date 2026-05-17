package forge.engine;

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
import forge.strategy.StrategyCatalog;
import forge.strategy.TradingStrategy;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
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
                (windows, batchSize) -> FacadeForgeData.getTheInstance()
                        .forgeDataAccess()
                        .openTradeBatchReader(windows, batchSize),
                new StrategyCatalog(),
                new ContractNameResolver(),
                new StaticFuturesInstrumentSpecProvider()
        );
    }

    public BacktestEngine(TickDataProvider tickDataProvider) {
        this(
                Objects.requireNonNull(tickDataProvider, "tickDataProvider is required")::openReader,
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
        Objects.requireNonNull(request, "request is required");
        TradingStrategy strategy = createStrategy(request.getStrategyOptions().getStrategyName());
        TradeBatchReader reader = tradeBatchReaderFactory.openReader(request.getContractWindows(), DEFAULT_BATCH_SIZE);
        Map<String, FuturesInstrumentSpec> specsByInstrument = new HashMap<>();

        long ticksProcessed = 0;
        long orderSignalsGenerated = 0;
        while (true) {
            List<TradeTick> batch = reader.readNextBatch();
            if (batch.isEmpty()) {
                break;
            }
            for (TradeTick tick : batch) {
                MarketContext marketContext = toMarketContext(tick, specsByInstrument);
                if (strategy.evaluate(marketContext).isPresent()) {
                    orderSignalsGenerated++;
                }
                ticksProcessed++;
            }
        }

        return new BacktestResult(
                request.getStrategyOptions().getStrategyName(),
                request.getInstruments(),
                ticksProcessed,
                orderSignalsGenerated
        );
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
        double lastPrice = tick.getPriceTicks() * spec.getTickSize();
        LocalDateTime timestamp = LocalDateTime.ofInstant(tick.getTradeDateTime(), ZoneOffset.UTC);
        return new MarketContext(instrumentSymbol, timestamp, lastPrice, false);
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
    }
}
