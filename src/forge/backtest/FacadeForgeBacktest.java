package forge.backtest;

public class FacadeForgeBacktest {
    private static final FacadeForgeBacktest THE_INSTANCE = new FacadeForgeBacktest();

    private final ForgeBacktestAccess access = new ForgeBacktestAccess();

    public static FacadeForgeBacktest getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeBacktestAccess forgeBacktestAccess() {
        return access;
    }

    public static class ForgeBacktestAccess {
        public Position createPosition() {
            return new Position();
        }

        public TradeResult createTradeResult() {
            return new TradeResult();
        }
    }
}
