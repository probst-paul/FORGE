package forge.trade;

import forge.trade.TradeResult;
import forge.data.market.TradeTick;
import forge.trade.Fill;
import forge.trade.OrderRequest;
import forge.trade.OrderSide;
import forge.model.FuturesInstrumentSpec;

import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

public class TradeLifecycleEngine {
    public static final String EXIT_REASON_TARGET = "TARGET";
    public static final String EXIT_REASON_STOP = "STOP";
    public static final String EXIT_REASON_TIME_STOP = "TIME_STOP";
    public static final String EXIT_REASON_END_OF_BACKTEST = "END_OF_BACKTEST";

    private Position openPosition;
    private TradePlan tradePlan;
    private TradeTick lastTick;

    public boolean hasOpenPosition() {
        return openPosition != null;
    }

    /*
     * Intent: Open a simulated position from an order request using the current tick as the entry fill.
     * Precondition: No position may already be open; request, plan, entry tick, contract symbol, and instrument spec must be valid.
     * Returns: Nothing.
     * Postcondition: Engine holds one open position, its trade plan, and the entry tick as the latest tick.
     */
    public void openPosition(
            OrderRequest orderRequest,
            TradePlan plan,
            TradeTick entryTick,
            String contractSymbol,
            FuturesInstrumentSpec instrumentSpec
    ) {
        Objects.requireNonNull(orderRequest, "orderRequest is required");
        Objects.requireNonNull(plan, "plan is required");
        Objects.requireNonNull(entryTick, "entryTick is required");
        Objects.requireNonNull(instrumentSpec, "instrumentSpec is required");
        if (hasOpenPosition()) {
            throw new IllegalStateException("position is already open");
        }
        if (orderRequest.getSide() != plan.getSide()) {
            throw new IllegalArgumentException("order side must match trade plan side");
        }
        this.tradePlan = plan;
        this.openPosition = new Position(
                orderRequest.getInstrumentSymbol(),
                contractSymbol,
                orderRequest.getSide(),
                entryTick.getTradeDateTime(),
                entryTick.getPriceTicks(),
                orderRequest.getQuantity(),
                instrumentSpec.getTickDollarAmount()
        );
        this.lastTick = entryTick;
        openPosition.updateExcursion(entryTick.getPriceTicks());
    }

    /*
     * Intent: Open a simulated position from an already-created fill.
     * Precondition: No position may already be open; fill, plan, and instrument spec must be valid; fill side must match plan side.
     * Returns: Nothing.
     * Postcondition: Engine holds one open position and its trade plan.
     */
    public void openPosition(
            Fill entryFill,
            TradePlan plan,
            FuturesInstrumentSpec instrumentSpec
    ) {
        Objects.requireNonNull(entryFill, "entryFill is required");
        Objects.requireNonNull(plan, "plan is required");
        Objects.requireNonNull(instrumentSpec, "instrumentSpec is required");
        if (hasOpenPosition()) {
            throw new IllegalStateException("position is already open");
        }
        if (entryFill.getSide() != plan.getSide()) {
            throw new IllegalArgumentException("fill side must match trade plan side");
        }
        this.tradePlan = plan;
        this.openPosition = new Position(
                entryFill.getInstrumentSymbol(),
                entryFill.getContractSymbol(),
                entryFill.getSide(),
                entryFill.getFillTime(),
                entryFill.getFillPriceTicks(),
                entryFill.getQuantity(),
                instrumentSpec.getTickDollarAmount()
        );
        openPosition.updateExcursion(entryFill.getFillPriceTicks());
    }

    /*
     * Intent: Advance the trade lifecycle by one tick and close the position if any exit condition is met.
     * Precondition: Tick must exist and must be ordered consistently by the caller.
     * Returns: Optional completed TradeResult when an exit is triggered; otherwise Optional.empty().
     * Postcondition: Last tick is updated, MFE/MAE are refreshed, and an exited position is cleared.
     */
    public Optional<TradeResult> onTick(TradeTick tick) {
        Objects.requireNonNull(tick, "tick is required");
        lastTick = tick;
        if (!hasOpenPosition()) {
            return Optional.empty();
        }

        openPosition.updateExcursion(tick.getPriceTicks());
        String exitReason = findExitReason(tick);
        if (exitReason == null) {
            return Optional.empty();
        }
        return close(tick, exitReason);
    }

    /*
     * Intent: Force-close any remaining open position at the final known tick.
     * Precondition: A position and last tick must exist to create an end-of-backtest exit.
     * Returns: Optional completed TradeResult when there is an open position; otherwise Optional.empty().
     * Postcondition: Any open position is cleared using END_OF_BACKTEST as the exit reason.
     */
    public Optional<TradeResult> closeOpenPositionAtEnd() {
        if (!hasOpenPosition() || lastTick == null) {
            return Optional.empty();
        }
        return close(lastTick, EXIT_REASON_END_OF_BACKTEST);
    }

    /*
     * Intent: Determine whether the current tick hits target, price stop, or time stop.
     * Precondition: A position and trade plan must be open; tick must exist.
     * Returns: Exit reason text when an exit is hit, or null when the trade stays open.
     * Postcondition: Engine state is unchanged.
     */
    private String findExitReason(TradeTick tick) {
        long priceTicks = tick.getPriceTicks();
        OrderSide side = openPosition.getSide();
        boolean targetHit = side == OrderSide.BUY
                ? priceTicks >= tradePlan.getTargetPriceTicks()
                : priceTicks <= tradePlan.getTargetPriceTicks();
        if (targetHit) {
            return EXIT_REASON_TARGET;
        }

        boolean stopHit = side == OrderSide.BUY
                ? priceTicks <= tradePlan.getStopPriceTicks()
                : priceTicks >= tradePlan.getStopPriceTicks();
        if (stopHit) {
            return EXIT_REASON_STOP;
        }

        LocalTime currentTime = tick.getTradeDateTime()
                .atZone(tradePlan.getTimeZone())
                .toLocalTime();
        if (!currentTime.isBefore(tradePlan.getTimeStop())) {
            return EXIT_REASON_TIME_STOP;
        }
        return null;
    }

    /*
     * Intent: Close the current open position at the supplied tick.
     * Precondition: An open position must exist; tick and exit reason must be valid.
     * Returns: Completed TradeResult wrapped in Optional.
     * Postcondition: Open position and trade plan are cleared.
     */
    private Optional<TradeResult> close(TradeTick tick, String exitReason) {
        TradeResult trade = openPosition.close(
                tick.getTradeDateTime(),
                tick.getPriceTicks(),
                exitReason
        );
        openPosition = null;
        tradePlan = null;
        return Optional.of(trade);
    }
}
