package forge.trade;

import java.util.Objects;

public class OrderRequest {
    private final String instrumentSymbol;
    private final OrderSide side;
    private final OrderType orderType;
    private final int quantity;
    private final Double limitPrice;
    private final Double stopPrice;

    public OrderRequest(
            String instrumentSymbol,
            OrderSide side,
            OrderType orderType,
            int quantity,
            Double limitPrice,
            Double stopPrice
    ) {
        /*
         * Intent: Describe an order the strategy wants simulated execution to attempt.
         * Precondition: Instrument symbol must be nonblank; side/order type must exist; quantity must be positive.
         * Returns: A constructed OrderRequest instance.
         * Postcondition: Order request fields are normalized and validated before reaching the execution engine.
         */
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }

        this.instrumentSymbol = instrumentSymbol.trim().toUpperCase();
        this.side = Objects.requireNonNull(side, "side is required");
        this.orderType = Objects.requireNonNull(orderType, "orderType is required");
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.stopPrice = stopPrice;
    }

    /*
     * Intent: Build the common market-order request without exposing limit/stop placeholders to callers.
     * Precondition: Instrument symbol, side, and quantity must satisfy the OrderRequest constructor validation.
     * Returns: A market OrderRequest with no limit or stop price.
     * Postcondition: The returned request is ready for immediate simulated market execution.
     */
    public static OrderRequest market(String instrumentSymbol, OrderSide side, int quantity) {
        return new OrderRequest(instrumentSymbol, side, OrderType.MARKET, quantity, null, null);
    }

    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public int getQuantity() {
        return quantity;
    }

    public Double getLimitPrice() {
        return limitPrice;
    }

    public Double getStopPrice() {
        return stopPrice;
    }

    /*
     * Intent: Provide a readable representation for debugging and logs.
     * Precondition: OrderRequest has been constructed successfully.
     * Returns: Text containing the order request fields.
     * Postcondition: Object state is unchanged.
     */
    @Override
    public String toString() {
        return "OrderRequest{" +
                "instrumentSymbol='" + instrumentSymbol + '\'' +
                ", side=" + side +
                ", orderType=" + orderType +
                ", quantity=" + quantity +
                ", limitPrice=" + limitPrice +
                ", stopPrice=" + stopPrice +
                '}';
    }
}
