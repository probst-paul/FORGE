package forge.target;

import forge.execution.OrderSide;

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
    public TargetResult calculateTarget(OrderSide side, long entryPriceTicks, long stopPriceTicks) {
        validateInputs(side, entryPriceTicks, stopPriceTicks);

        long targetPriceTicks = side == OrderSide.BUY
                ? entryPriceTicks + targetTicks
                : entryPriceTicks - targetTicks;

        return new TargetResult(targetPriceTicks, stopPriceTicks);
    }

    public int getTargetTicks() {
        return targetTicks;
    }

    private void validateInputs(OrderSide side, long entryPriceTicks, long stopPriceTicks) {
        if (side == null) {
            throw new NullPointerException("side is required");
        }
        if (entryPriceTicks <= 0) {
            throw new IllegalArgumentException("entryPriceTicks must be greater than zero");
        }
        if (stopPriceTicks <= 0) {
            throw new IllegalArgumentException("stopPriceTicks must be greater than zero");
        }
    }
}
