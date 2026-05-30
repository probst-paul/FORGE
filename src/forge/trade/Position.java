package forge.trade;

import forge.model.FuturesInstrumentSpec;
import forge.trade.TradeResult;
import forge.trade.OrderSide;

import java.time.Instant;
import java.util.Objects;

public class Position {
    private final String instrumentSymbol;
    private final String contractSymbol;
    private final OrderSide side;
    private final Instant entryTime;
    private final long entryPriceTicks;
    private final int quantity;
    private final double tickSize;
    private final double tickDollarValue;
    private long maxFavorableExcursionTicks;
    private long maxAdverseExcursionTicks;

    public Position(
            String instrumentSymbol,
            String contractSymbol,
            OrderSide side,
            Instant entryTime,
            long entryPriceTicks,
            int quantity,
            double tickSize,
            double tickDollarValue
    ) {
        /*
         * Intent: Represent an open simulated position and track its excursion from entry.
         * Precondition: Symbols must be nonblank; side/entry time must exist; entry ticks, quantity, and tick dollar value must be positive.
         * Returns: A constructed Position instance.
         * Postcondition: Position starts open with zero favorable/adverse excursion.
         */
        this.instrumentSymbol = requireText(instrumentSymbol, "instrumentSymbol is required");
        this.contractSymbol = requireText(contractSymbol, "contractSymbol is required");
        this.side = Objects.requireNonNull(side, "side is required");
        this.entryTime = Objects.requireNonNull(entryTime, "entryTime is required");
        if (entryPriceTicks <= 0) {
            throw new IllegalArgumentException("entryPriceTicks must be greater than zero");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
        if (tickDollarValue <= 0) {
            throw new IllegalArgumentException("tickDollarValue must be greater than zero");
        }
        this.entryPriceTicks = entryPriceTicks;
        this.quantity = quantity;
        this.tickSize = tickSize;
        this.tickDollarValue = tickDollarValue;
    }

    /*
     * Intent: Update maximum favorable and adverse movement seen while the position is open.
     * Precondition: priceTicks must be positive and measured in the same tick scale as the entry.
     * Returns: Nothing.
     * Postcondition: MFE/MAE tick fields only increase when the new price extends prior extremes.
     */
    public void updateExcursion(long priceTicks) {
        if (priceTicks <= 0) {
            throw new IllegalArgumentException("priceTicks must be greater than zero");
        }
        long signedMovementTicks = FuturesInstrumentSpec.contractTicksBetween(
                entryPriceTicks,
                priceTicks,
                tickSize
        );
        long favorableTicks;
        long adverseTicks;
        if (side == OrderSide.BUY) {
            favorableTicks = Math.max(0, signedMovementTicks);
            adverseTicks = Math.max(0, -signedMovementTicks);
        } else {
            favorableTicks = Math.max(0, -signedMovementTicks);
            adverseTicks = Math.max(0, signedMovementTicks);
        }
        maxFavorableExcursionTicks = Math.max(maxFavorableExcursionTicks, favorableTicks);
        maxAdverseExcursionTicks = Math.max(maxAdverseExcursionTicks, adverseTicks);
    }

    /*
     * Intent: Close the open position and convert position state into a completed trade result.
     * Precondition: exitTime must exist, exitPriceTicks must be positive, and exitReason must be valid for TradeResult.
     * Returns: A TradeResult containing P/L, MFE, MAE, and exit metadata.
     * Postcondition: Position object remains readable, and excursion includes the exit price before result creation.
     */
    public TradeResult close(Instant exitTime, long exitPriceTicks, String exitReason) {
        Objects.requireNonNull(exitTime, "exitTime is required");
        if (exitPriceTicks <= 0) {
            throw new IllegalArgumentException("exitPriceTicks must be greater than zero");
        }
        updateExcursion(exitPriceTicks);
        long signedMovementTicks = FuturesInstrumentSpec.contractTicksBetween(
                entryPriceTicks,
                exitPriceTicks,
                tickSize
        );
        long grossTicks = side == OrderSide.BUY
                ? signedMovementTicks * quantity
                : -signedMovementTicks * quantity;
        return new TradeResult(
                instrumentSymbol,
                contractSymbol,
                side,
                entryTime,
                entryPriceTicks,
                exitTime,
                exitPriceTicks,
                quantity,
                grossTicks,
                grossTicks * tickDollarValue,
                maxFavorableExcursionTicks * quantity * tickDollarValue,
                -maxAdverseExcursionTicks * quantity * tickDollarValue,
                exitReason
        );
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

    public int getQuantity() {
        return quantity;
    }

    public long getMaxFavorableExcursionTicks() {
        return maxFavorableExcursionTicks;
    }

    public long getMaxAdverseExcursionTicks() {
        return maxAdverseExcursionTicks;
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
