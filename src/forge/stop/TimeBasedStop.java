package forge.stop;

import forge.engine.MarketContext;
import forge.execution.OrderSide;

import java.time.LocalTime;
import java.util.Objects;

public class TimeBasedStop implements StopModel {
    private final LocalTime exitTime;

    public TimeBasedStop() {
        this(LocalTime.of(15, 55));
    }

    public TimeBasedStop(LocalTime exitTime) {
        this.exitTime = Objects.requireNonNull(exitTime, "exitTime is required");
    }

    @Override
    public String getName() {
        return "Time Based";
    }

    @Override
    public StopResult evaluateStop(OrderSide positionSide, MarketContext marketContext) {
        Objects.requireNonNull(positionSide, "positionSide is required");
        Objects.requireNonNull(marketContext, "marketContext is required");

        LocalTime currentTime = marketContext.getTimestamp().toLocalTime();
        if (currentTime.isBefore(exitTime)) {
            return StopResult.notStopped();
        }
        return StopResult.stopped(StopReason.TIME, marketContext.getLastPriceTicks());
    }

    public LocalTime getExitTime() {
        return exitTime;
    }
}
