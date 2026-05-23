package forge.trade;

import forge.trade.TradeResult;
import forge.data.market.TradeTick;
import forge.execution.Fill;
import forge.execution.OrderRequest;
import forge.execution.OrderSide;
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

    public Optional<TradeResult> closeOpenPositionAtEnd() {
        if (!hasOpenPosition() || lastTick == null) {
            return Optional.empty();
        }
        return close(lastTick, EXIT_REASON_END_OF_BACKTEST);
    }

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
