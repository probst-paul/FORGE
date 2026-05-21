package forge.execution;

import java.time.Instant;

public class FacadeForgeExecution {
    private static final FacadeForgeExecution THE_INSTANCE = new FacadeForgeExecution();

    private final ForgeExecutionAccess access = new ForgeExecutionAccess();

    public static FacadeForgeExecution getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeExecutionAccess forgeExecutionAccess() {
        return access;
    }

    public static class ForgeExecutionAccess {
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
    }
}
