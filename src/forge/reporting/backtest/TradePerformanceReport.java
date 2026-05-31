package forge.reporting.backtest;

import forge.trade.OrderSide;
import forge.trade.TradeResult;

import java.time.Instant;
import java.util.Objects;

public class TradePerformanceReport {
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

    public TradePerformanceReport(TradeResult tradeResult) {
        /*
         * Intent: Convert one simulated trade result into a stable report row.
         * Precondition: tradeResult must be non-null and already validated by TradeResult.
         * Returns: Constructed TradePerformanceReport.
         * Postcondition: Trade fields are copied so report consumers do not depend directly on lifecycle objects.
         */
        TradeResult trade = Objects.requireNonNull(tradeResult, "tradeResult is required");
        this.instrumentSymbol = trade.getInstrumentSymbol();
        this.contractSymbol = trade.getContractSymbol();
        this.side = trade.getSide();
        this.entryTime = trade.getEntryTime();
        this.entryPriceTicks = trade.getEntryPriceTicks();
        this.exitTime = trade.getExitTime();
        this.exitPriceTicks = trade.getExitPriceTicks();
        this.quantity = trade.getQuantity();
        this.grossTicks = trade.getGrossTicks();
        this.grossDollars = trade.getGrossDollars();
        this.maxFavorableExcursionDollars = trade.getMaxFavorableExcursionDollars();
        this.maxAdverseExcursionDollars = trade.getMaxAdverseExcursionDollars();
        this.exitReason = trade.getExitReason();
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
}
