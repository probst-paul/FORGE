package forge.trade;

import java.time.Instant;
import java.util.Objects;

public class Fill {
    private final String instrumentSymbol;
    private final String contractSymbol;
    private final OrderSide side;
    private final OrderType orderType;
    private final int quantity;
    private final Instant fillTime;
    private final long fillPriceTicks;
    private final long scidRecordIndex;

    public Fill(
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
         * Intent: Create an immutable record of an executed order fill.
         * Precondition: Symbols must be nonblank; side/order type/fill time must exist; quantity, price ticks, and record index must be positive.
         * Returns: A constructed Fill instance.
         * Postcondition: Fill fields are normalized and validated for later trade lifecycle use.
         */
        this.instrumentSymbol = requireText(instrumentSymbol, "instrumentSymbol is required");
        this.contractSymbol = requireText(contractSymbol, "contractSymbol is required");
        this.side = Objects.requireNonNull(side, "side is required");
        this.orderType = Objects.requireNonNull(orderType, "orderType is required");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (fillPriceTicks <= 0) {
            throw new IllegalArgumentException("fillPriceTicks must be greater than zero");
        }
        if (scidRecordIndex < 1) {
            throw new IllegalArgumentException("scidRecordIndex must be positive");
        }
        this.quantity = quantity;
        this.fillTime = Objects.requireNonNull(fillTime, "fillTime is required");
        this.fillPriceTicks = fillPriceTicks;
        this.scidRecordIndex = scidRecordIndex;
    }

    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }

    public String getContractSymbol() {
        return contractSymbol;
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

    public Instant getFillTime() {
        return fillTime;
    }

    public long getFillPriceTicks() {
        return fillPriceTicks;
    }

    public long getScidRecordIndex() {
        return scidRecordIndex;
    }

    /*
     * Intent: Validate and normalize required symbol-like text.
     * Precondition: Value must not be null, empty, or whitespace-only.
     * Returns: Trimmed uppercase text.
     * Postcondition: Callers receive canonical text or an exception before invalid state is stored.
     */
    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase();
    }
}
