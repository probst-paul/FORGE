package forge.config;

public class RiskSettings {
    private final double riskPerTrade;
    private final double maxDailyLoss;

    public RiskSettings(double riskPerTrade, double maxDailyLoss) {
        if (riskPerTrade <= 0) {
            throw new IllegalArgumentException("riskPerTrade must be greater than zero");
        }
        if (maxDailyLoss < 0) {
            throw new IllegalArgumentException("maxDailyLoss cannot be negative");
        }

        this.riskPerTrade = riskPerTrade;
        this.maxDailyLoss = maxDailyLoss;
    }

    public double getRiskPerTrade() {
        return riskPerTrade;
    }

    public double getMaxDailyLoss() {
        return maxDailyLoss;
    }

    @Override
    public String toString() {
        return "RiskSettings{" +
                "riskPerTrade=" + riskPerTrade +
                ", maxDailyLoss=" + maxDailyLoss +
                '}';
    }
}
