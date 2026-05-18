package forge.stop;

import forge.engine.MarketContext;
import forge.execution.OrderSide;

import java.util.Objects;

public class PriceBasedStop implements StopModel {
    private final long stopPriceTicks;

    public PriceBasedStop() {
        this(1);
    }

    public PriceBasedStop(long stopPriceTicks) {
        if (stopPriceTicks <= 0) {
            throw new IllegalArgumentException("stopPriceTicks must be greater than zero");
        }
        this.stopPriceTicks = stopPriceTicks;
    }

    @Override
    public String getName() {
        return "Price Based";
    }

    @Override
    public StopResult evaluateStop(OrderSide positionSide, MarketContext marketContext) {
        Objects.requireNonNull(positionSide, "positionSide is required");
        Objects.requireNonNull(marketContext, "marketContext is required");

        long lastPriceTicks = marketContext.getLastPriceTicks();
        boolean stopped = positionSide == OrderSide.BUY
                ? lastPriceTicks <= stopPriceTicks
                : lastPriceTicks >= stopPriceTicks;
        if (!stopped) {
            return StopResult.notStopped();
        }
        return StopResult.stopped(StopReason.PRICE, lastPriceTicks);
    }

    public long getStopPriceTicks() {
        return stopPriceTicks;
    }
}
