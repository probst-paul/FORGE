package forge.trade;

import forge.execution.OrderSide;

import java.time.LocalTime;
import java.time.ZoneId;

public class FacadeForgeTrade {
    private static final FacadeForgeTrade THE_INSTANCE = new FacadeForgeTrade();

    private final ForgeTradeAccess access = new ForgeTradeAccess();

    public static FacadeForgeTrade getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeTradeAccess forgeTradeAccess() {
        return access;
    }

    public static class ForgeTradeAccess {
        public TradeLifecycleEngine createTradeLifecycleEngine() {
            return new TradeLifecycleEngine();
        }

        public TradePlan createTradePlan(
                OrderSide side,
                long targetPriceTicks,
                long stopPriceTicks,
                LocalTime timeStop,
                ZoneId timeZone
        ) {
            return new TradePlan(side, targetPriceTicks, stopPriceTicks, timeStop, timeZone);
        }
    }
}
