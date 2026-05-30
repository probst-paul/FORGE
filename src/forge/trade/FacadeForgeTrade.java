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
        /*
         * Intent: Create a fresh trade lifecycle engine for one backtest/run scope.
         * Precondition: None.
         * Returns: New TradeLifecycleEngine.
         * Postcondition: Trade state is isolated to the returned engine instance.
         */
        public TradeLifecycleEngine createTradeLifecycleEngine() {
            return new TradeLifecycleEngine();
        }

        /*
         * Intent: Create the MVP execution engine that fills at the current tick.
         * Precondition: None.
         * Returns: ExecutionEngine implementation for simple market fills.
         * Postcondition: Callers depend on the execution abstraction, not the concrete engine class.
         */
        public ExecutionEngine createSimpleExecutionEngine() {
            return new SimpleExecutionEngine();
        }

        /*
         * Intent: Create a validated market order request through the trade facade.
         * Precondition: Instrument symbol, side, and quantity must satisfy OrderRequest validation.
         * Returns: Market OrderRequest.
         * Postcondition: Order construction details remain centralized in the trade package.
         */
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
            /*
             * Intent: Create a validated fill through the trade facade.
             * Precondition: Fill arguments must satisfy Fill constructor validation.
             * Returns: Fill representing one simulated execution.
             * Postcondition: Fill construction details remain centralized in the trade package.
             */
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
            /*
             * Intent: Create immutable exit rules for one trade.
             * Precondition: Side, target, stop, time stop, and time zone must be valid.
             * Returns: TradePlan ready for lifecycle evaluation.
             * Postcondition: Trade-plan validation stays inside the trade package.
             */
            return new TradePlan(side, targetPriceTicks, stopPriceTicks, timeStop, timeZone);
        }
    }
}
