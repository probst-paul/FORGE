package forge.target;

import forge.execution.OrderSide;

public class FixedRiskRewardTarget implements TargetModel {
    private final double rewardRiskRatio;

    public FixedRiskRewardTarget() {
        this(2.0);
    }

    public FixedRiskRewardTarget(double rewardRiskRatio) {
        if (rewardRiskRatio <= 0) {
            throw new IllegalArgumentException("rewardRiskRatio must be greater than zero");
        }
        this.rewardRiskRatio = rewardRiskRatio;
    }

    @Override
    public String getName() {
        return "Fixed Risk/Reward";
    }

    @Override
    public TargetResult calculateTarget(OrderSide side, long entryPriceTicks, long stopPriceTicks) {
        validateInputs(side, entryPriceTicks, stopPriceTicks);

        long riskTicks = Math.abs(entryPriceTicks - stopPriceTicks);
        long targetDistanceTicks = Math.round(riskTicks * rewardRiskRatio);
        long targetPriceTicks = side == OrderSide.BUY
                ? entryPriceTicks + targetDistanceTicks
                : entryPriceTicks - targetDistanceTicks;

        return new TargetResult(targetPriceTicks, stopPriceTicks);
    }

    public double getRewardRiskRatio() {
        return rewardRiskRatio;
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
        if (entryPriceTicks == stopPriceTicks) {
            throw new IllegalArgumentException("entryPriceTicks and stopPriceTicks cannot be equal");
        }
        if (side == OrderSide.BUY && stopPriceTicks >= entryPriceTicks) {
            throw new IllegalArgumentException("buy stopPriceTicks must be below entryPriceTicks");
        }
        if (side == OrderSide.SELL && stopPriceTicks <= entryPriceTicks) {
            throw new IllegalArgumentException("sell stopPriceTicks must be above entryPriceTicks");
        }
    }
}
