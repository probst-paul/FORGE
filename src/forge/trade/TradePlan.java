package forge.trade;

import forge.trade.OrderSide;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

public class TradePlan {
    private final OrderSide side;
    private final long targetPriceTicks;
    private final long stopPriceTicks;
    private final LocalTime timeStop;
    private final ZoneId timeZone;

    public TradePlan(
            OrderSide side,
            long targetPriceTicks,
            long stopPriceTicks,
            LocalTime timeStop,
            ZoneId timeZone
    ) {
        /*
         * Intent: Define the target, stop, and time-stop rules for one simulated trade.
         * Precondition: Side, time stop, and time zone must exist; target and stop prices must be positive tick values.
         * Returns: A constructed TradePlan instance.
         * Postcondition: Trade exit rules are immutable and ready for lifecycle evaluation.
         */
        this.side = Objects.requireNonNull(side, "side is required");
        if (targetPriceTicks <= 0) {
            throw new IllegalArgumentException("targetPriceTicks must be greater than zero");
        }
        if (stopPriceTicks <= 0) {
            throw new IllegalArgumentException("stopPriceTicks must be greater than zero");
        }
        this.targetPriceTicks = targetPriceTicks;
        this.stopPriceTicks = stopPriceTicks;
        this.timeStop = Objects.requireNonNull(timeStop, "timeStop is required");
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone is required");
    }

    public OrderSide getSide() {
        return side;
    }

    public long getTargetPriceTicks() {
        return targetPriceTicks;
    }

    public long getStopPriceTicks() {
        return stopPriceTicks;
    }

    public LocalTime getTimeStop() {
        return timeStop;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }
}
