package forge.execution;

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
