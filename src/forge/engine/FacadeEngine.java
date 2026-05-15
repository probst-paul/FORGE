package forge.engine;

import java.time.LocalDateTime;

public class FacadeEngine {
    private final BacktestEngine backtestEngine;

    public FacadeEngine() {
        this(new BacktestEngine());
    }

    public FacadeEngine(BacktestEngine backtestEngine) {
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
