package forge.config;

import forge.trade.OrderType;

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
        /*
         * Intent: Store order-entry settings used when a strategy signal becomes a simulated order.
         * Precondition: Order type must be provided, quantity must be positive, and offsets cannot be negative.
         * Returns: A constructed OrderSettings instance.
         * Postcondition: Order settings are immutable and validated for execution use.
         */
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
        /*
         * Intent: Provide a readable diagnostic summary of order settings.
         * Precondition: OrderSettings must be constructed.
         * Returns: String representation of order settings.
         * Postcondition: OrderSettings state is unchanged.
         */
        return "OrderSettings{" +
                "entryOrderType=" + entryOrderType +
                ", quantity=" + quantity +
                ", limitOffsetTicks=" + limitOffsetTicks +
                ", stopOffsetTicks=" + stopOffsetTicks +
                '}';
    }
}
