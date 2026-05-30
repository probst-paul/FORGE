package forge.config;

public class RiskSettings {
    private final double riskPerTrade;
    private final double maxDailyLoss;

    public RiskSettings(double riskPerTrade, double maxDailyLoss) {
        /*
         * Intent: Store per-trade and daily risk limits for strategy execution.
         * Precondition: Risk per trade must be positive and max daily loss cannot be negative.
         * Returns: A constructed RiskSettings instance.
         * Postcondition: Risk settings are immutable and validated for backtest use.
         */
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
        /*
         * Intent: Provide a readable diagnostic summary of risk settings.
         * Precondition: RiskSettings must be constructed.
         * Returns: String representation of risk settings.
         * Postcondition: RiskSettings state is unchanged.
         */
        return "RiskSettings{" +
                "riskPerTrade=" + riskPerTrade +
                ", maxDailyLoss=" + maxDailyLoss +
                '}';
    }
}
