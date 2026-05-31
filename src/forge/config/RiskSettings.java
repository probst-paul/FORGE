package forge.config;

public class RiskSettings {
    private final boolean perTradeRiskEnabled;
    private final double riskPerTrade;
    private final boolean dailyRiskEnabled;
    private final double maxDailyLoss;

    public RiskSettings(double riskPerTrade, double maxDailyLoss) {
        this(true, riskPerTrade, true, maxDailyLoss);
    }

    public RiskSettings(
            boolean perTradeRiskEnabled,
            double riskPerTrade,
            boolean dailyRiskEnabled,
            double maxDailyLoss
    ) {
        /*
         * Intent: Store per-trade and daily risk limits for strategy execution.
         * Precondition: Enabled risk limits must be positive; disabled limits are ignored.
         * Returns: A constructed RiskSettings instance.
         * Postcondition: Risk settings are immutable and validated for backtest use.
         */
        if (riskPerTrade < 0) {
            throw new IllegalArgumentException("riskPerTrade cannot be negative");
        }
        if (maxDailyLoss < 0) {
            throw new IllegalArgumentException("maxDailyLoss cannot be negative");
        }
        if (perTradeRiskEnabled && riskPerTrade <= 0) {
            throw new IllegalArgumentException("riskPerTrade must be greater than zero when per-trade risk is enabled");
        }
        if (dailyRiskEnabled && maxDailyLoss <= 0) {
            throw new IllegalArgumentException("maxDailyLoss must be greater than zero when daily risk is enabled");
        }
        if (perTradeRiskEnabled && dailyRiskEnabled && maxDailyLoss < riskPerTrade) {
            throw new IllegalArgumentException("maxDailyLoss cannot be less than riskPerTrade");
        }

        this.perTradeRiskEnabled = perTradeRiskEnabled;
        this.riskPerTrade = riskPerTrade;
        this.dailyRiskEnabled = dailyRiskEnabled;
        this.maxDailyLoss = maxDailyLoss;
    }

    public boolean isPerTradeRiskEnabled() {
        return perTradeRiskEnabled;
    }

    public double getRiskPerTrade() {
        return riskPerTrade;
    }

    public boolean isDailyRiskEnabled() {
        return dailyRiskEnabled;
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
                "perTradeRiskEnabled=" + perTradeRiskEnabled +
                ", riskPerTrade=" + riskPerTrade +
                ", dailyRiskEnabled=" + dailyRiskEnabled +
                ", maxDailyLoss=" + maxDailyLoss +
                '}';
    }
}
