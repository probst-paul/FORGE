package forge.execution;

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

        public Fill createFill() {
            return new Fill();
        }
    }
}
