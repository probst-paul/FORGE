package forge.trade;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class TradeResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String instrumentSymbol;
    private final String contractSymbol;
    private final OrderSide side;
    private final Instant entryTime;
    private final long entryPriceTicks;
    private final Instant exitTime;
    private final long exitPriceTicks;
    private final int quantity;
    private final long grossTicks;
    private final double grossDollars;
    private final double maxFavorableExcursionDollars;
    private final double maxAdverseExcursionDollars;
    private final String exitReason;

    public TradeResult() {
        this(
                "ES",
                "ESU25",
                OrderSide.BUY,
                Instant.EPOCH,
                1,
                Instant.EPOCH,
                1,
                1,
                0,
                0,
                0,
                0,
                "PLACEHOLDER"
        );
    }

    public TradeResult(
            String instrumentSymbol,
            String contractSymbol,
            OrderSide side,
            Instant entryTime,
            long entryPriceTicks,
            Instant exitTime,
            long exitPriceTicks,
            int quantity,
            long grossTicks,
            double grossDollars,
            double maxFavorableExcursionDollars,
            double maxAdverseExcursionDollars,
            String exitReason
    ) {
        /*
         * Intent: Store the final outcome of one completed simulated trade.
         * Precondition: Symbols and exit reason must be nonblank; side/times must exist; prices and quantity must be positive.
         * Returns: A constructed TradeResult instance.
         * Postcondition: Trade result fields are validated and normalized for reporting.
         */
        this.instrumentSymbol = requireText(instrumentSymbol, "instrumentSymbol is required");
        this.contractSymbol = requireText(contractSymbol, "contractSymbol is required");
        this.side = Objects.requireNonNull(side, "side is required");
        this.entryTime = Objects.requireNonNull(entryTime, "entryTime is required");
        this.exitTime = Objects.requireNonNull(exitTime, "exitTime is required");
        if (entryPriceTicks <= 0) {
            throw new IllegalArgumentException("entryPriceTicks must be positive");
        }
        if (exitPriceTicks <= 0) {
            throw new IllegalArgumentException("exitPriceTicks must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.entryPriceTicks = entryPriceTicks;
        this.exitPriceTicks = exitPriceTicks;
        this.quantity = quantity;
        this.grossTicks = grossTicks;
        this.grossDollars = grossDollars;
        this.maxFavorableExcursionDollars = maxFavorableExcursionDollars;
        this.maxAdverseExcursionDollars = maxAdverseExcursionDollars;
        this.exitReason = requireText(exitReason, "exitReason is required");
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

    public Instant getEntryTime() {
        return entryTime;
    }

    public long getEntryPriceTicks() {
        return entryPriceTicks;
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public long getExitPriceTicks() {
        return exitPriceTicks;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getGrossTicks() {
        return grossTicks;
    }

    public double getGrossDollars() {
        return grossDollars;
    }

    public double getMaxFavorableExcursionDollars() {
        return maxFavorableExcursionDollars;
    }

    public double getMaxAdverseExcursionDollars() {
        return maxAdverseExcursionDollars;
    }

    public String getExitReason() {
        return exitReason;
    }

    /*
     * Intent: Validate and normalize required report text.
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
