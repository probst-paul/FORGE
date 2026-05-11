package forge.target;

import forge.execution.OrderSide;

import java.util.Objects;

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
    public TargetResult calculateTarget(OrderSide side, double entryPrice, double stopPrice, double tickSize) {
        validateInputs(side, entryPrice, stopPrice, tickSize);

        double risk = Math.abs(entryPrice - stopPrice);
        double targetDistance = risk * rewardRiskRatio;
        double targetPrice = side == OrderSide.BUY
                ? entryPrice + targetDistance
                : entryPrice - targetDistance;

        return new TargetResult(roundToTick(targetPrice, tickSize), roundToTick(stopPrice, tickSize));
    }

    public double getRewardRiskRatio() {
        return rewardRiskRatio;
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
        if (entryPrice == stopPrice) {
            throw new IllegalArgumentException("entryPrice and stopPrice cannot be equal");
        }
        if (side == OrderSide.BUY && stopPrice >= entryPrice) {
            throw new IllegalArgumentException("buy stopPrice must be below entryPrice");
        }
        if (side == OrderSide.SELL && stopPrice <= entryPrice) {
            throw new IllegalArgumentException("sell stopPrice must be above entryPrice");
        }
    }

    private double roundToTick(double price, double tickSize) {
        return Math.round(price / tickSize) * tickSize;
    }
}
