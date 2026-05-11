package forge.config;

import forge.execution.OrderType;

import java.util.Objects;

public class OrderSettings {
    private final OrderType entryOrderType;
    private final int quantity;
    private final double limitOffsetTicks;
    private final double stopOffsetTicks;

    public OrderSettings(
            OrderType entryOrderType,
            int quantity,
            double limitOffsetTicks,
            double stopOffsetTicks
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (limitOffsetTicks < 0) {
            throw new IllegalArgumentException("limitOffsetTicks cannot be negative");
        }
        if (stopOffsetTicks < 0) {
            throw new IllegalArgumentException("stopOffsetTicks cannot be negative");
        }

        this.entryOrderType = Objects.requireNonNull(entryOrderType, "entryOrderType is required");
        this.quantity = quantity;
        this.limitOffsetTicks = limitOffsetTicks;
        this.stopOffsetTicks = stopOffsetTicks;
    }

    public OrderType getEntryOrderType() {
        return entryOrderType;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getLimitOffsetTicks() {
        return limitOffsetTicks;
    }

    public double getStopOffsetTicks() {
        return stopOffsetTicks;
    }

    @Override
    public String toString() {
        return "OrderSettings{" +
                "entryOrderType=" + entryOrderType +
                ", quantity=" + quantity +
                ", limitOffsetTicks=" + limitOffsetTicks +
                ", stopOffsetTicks=" + stopOffsetTicks +
                '}';
    }
}
