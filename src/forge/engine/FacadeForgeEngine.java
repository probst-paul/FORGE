package forge.engine;

import java.time.LocalDateTime;

public class FacadeForgeEngine {
    private static final FacadeForgeEngine THE_INSTANCE = new FacadeForgeEngine();

    private final BacktestEngine backtestEngine;

    public static FacadeForgeEngine getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeEngine() {
        this(new BacktestEngine());
    }

    public FacadeForgeEngine(BacktestEngine backtestEngine) {
        this.backtestEngine = backtestEngine;
    }

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
}
