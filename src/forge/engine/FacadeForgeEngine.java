package forge.engine;

import forge.app.BacktestProgressListener;
import forge.config.BacktestRequest;
import forge.reporting.BacktestResult;

import java.time.LocalDateTime;

public class FacadeForgeEngine {
    private static final FacadeForgeEngine THE_INSTANCE = new FacadeForgeEngine();

    private final BacktestEngine backtestEngine;
    private final ForgeEngineAccess access = new ForgeEngineAccess();

    public static FacadeForgeEngine getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeEngine() {
        this(new BacktestEngine());
    }

    public FacadeForgeEngine(BacktestEngine backtestEngine) {
        this.backtestEngine = backtestEngine;
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

        public BacktestResult run(BacktestRequest request) {
            return backtestEngine.run(request);
        }

        public BacktestResult run(BacktestRequest request, BacktestProgressListener progressListener) {
            return backtestEngine.run(request, progressListener);
        }
    }
}
