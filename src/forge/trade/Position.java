package forge.trade;

import forge.trade.TradeResult;
import forge.execution.OrderSide;

import java.time.Instant;
import java.util.Objects;

public class Position {
    private final String instrumentSymbol;
    private final String contractSymbol;
    private final OrderSide side;
    private final Instant entryTime;
    private final long entryPriceTicks;
    private final int quantity;
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
            double tickDollarValue
    ) {
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
        if (tickDollarValue <= 0) {
            throw new IllegalArgumentException("tickDollarValue must be greater than zero");
        }
        this.entryPriceTicks = entryPriceTicks;
        this.quantity = quantity;
        this.tickDollarValue = tickDollarValue;
    }

    public void updateExcursion(long priceTicks) {
        if (priceTicks <= 0) {
            throw new IllegalArgumentException("priceTicks must be greater than zero");
        }
        long favorableTicks;
        long adverseTicks;
        if (side == OrderSide.BUY) {
            favorableTicks = Math.max(0, priceTicks - entryPriceTicks);
            adverseTicks = Math.max(0, entryPriceTicks - priceTicks);
        } else {
            favorableTicks = Math.max(0, entryPriceTicks - priceTicks);
            adverseTicks = Math.max(0, priceTicks - entryPriceTicks);
        }
        maxFavorableExcursionTicks = Math.max(maxFavorableExcursionTicks, favorableTicks);
        maxAdverseExcursionTicks = Math.max(maxAdverseExcursionTicks, adverseTicks);
    }

    public TradeResult close(Instant exitTime, long exitPriceTicks, String exitReason) {
        Objects.requireNonNull(exitTime, "exitTime is required");
        if (exitPriceTicks <= 0) {
            throw new IllegalArgumentException("exitPriceTicks must be greater than zero");
        }
        updateExcursion(exitPriceTicks);
        long grossTicks = side == OrderSide.BUY
                ? (exitPriceTicks - entryPriceTicks) * quantity
                : (entryPriceTicks - exitPriceTicks) * quantity;
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

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase();
    }
}
