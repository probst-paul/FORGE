package forge.trade;

import java.time.Instant;
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

        public ExecutionEngine createSimpleExecutionEngine() {
            return new SimpleExecutionEngine();
        }

        public OrderRequest createMarketOrderRequest(String instrumentSymbol, OrderSide side, int quantity) {
            return OrderRequest.market(instrumentSymbol, side, quantity);
        }

        public Order createOrder() {
            return new Order();
        }

        public Fill createFill(
                String instrumentSymbol,
                String contractSymbol,
                OrderSide side,
                OrderType orderType,
                int quantity,
                Instant fillTime,
                long fillPriceTicks,
                long scidRecordIndex
        ) {
            return new Fill(
                    instrumentSymbol,
                    contractSymbol,
                    side,
                    orderType,
                    quantity,
                    fillTime,
                    fillPriceTicks,
                    scidRecordIndex
            );
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
