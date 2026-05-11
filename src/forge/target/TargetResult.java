package forge.target;

public class TargetResult {
    private final double targetPrice;
    private final double stopPrice;

    public TargetResult(double targetPrice, double stopPrice) {
        if (targetPrice <= 0) {
            throw new IllegalArgumentException("targetPrice must be greater than zero");
        }
        if (stopPrice <= 0) {
            throw new IllegalArgumentException("stopPrice must be greater than zero");
        }

        this.targetPrice = targetPrice;
        this.stopPrice = stopPrice;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    @Override
    public String toString() {
        return "TargetResult{" +
                "targetPrice=" + targetPrice +
                ", stopPrice=" + stopPrice +
                '}';
    }
}
