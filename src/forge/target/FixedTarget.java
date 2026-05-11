package forge.target;

import forge.execution.OrderSide;

import java.util.Objects;

public class FixedTarget implements TargetModel {
    private final int targetTicks;

    public FixedTarget() {
        this(8);
    }

    public FixedTarget(int targetTicks) {
        if (targetTicks <= 0) {
            throw new IllegalArgumentException("targetTicks must be greater than zero");
        }
        this.targetTicks = targetTicks;
    }

    @Override
    public String getName() {
        return "Target";
    }

    @Override
    public TargetResult calculateTarget(OrderSide side, double entryPrice, double stopPrice, double tickSize) {
        validateInputs(side, entryPrice, stopPrice, tickSize);

        double targetDistance = targetTicks * tickSize;
        double targetPrice = side == OrderSide.BUY
                ? entryPrice + targetDistance
                : entryPrice - targetDistance;

        return new TargetResult(roundToTick(targetPrice, tickSize), roundToTick(stopPrice, tickSize));
    }

    public int getTargetTicks() {
        return targetTicks;
    }

    private void validateInputs(OrderSide side, double entryPrice, double stopPrice, double tickSize) {
        Objects.requireNonNull(side, "side is required");
        if (entryPrice <= 0) {
            throw new IllegalArgumentException("entryPrice must be greater than zero");
        }
        if (stopPrice <= 0) {
            throw new IllegalArgumentException("stopPrice must be greater than zero");
        }
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
    }

    private double roundToTick(double price, double tickSize) {
        return Math.round(price / tickSize) * tickSize;
    }
}
