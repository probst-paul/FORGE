package forge.reporting;

import forge.backtest.TradeResult;

import java.util.List;

public class PerformanceMetrics {
    private final int totalTrades;
    private final int winningTrades;
    private final int losingTrades;
    private final double winRate;
    private final double netProfitLoss;
    private final double grossProfit;
    private final double grossLoss;
    private final double commissions;
    private final double averageTrade;
    private final double averageWinningTrade;
    private final double averageLosingTrade;
    private final double profitFactor;
    private final double maximumDrawdown;
    private final double maximumRunup;
    private final double maxFavorableExcursion;
    private final double averageFavorableExcursion;
    private final double maxAdverseExcursion;
    private final double averageAdverseExcursion;

    public PerformanceMetrics() {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public PerformanceMetrics(
            int totalTrades,
            int winningTrades,
            int losingTrades,
            double winRate,
            double netProfitLoss,
            double grossProfit,
            double grossLoss,
            double commissions,
            double averageTrade,
            double averageWinningTrade,
            double averageLosingTrade,
            double profitFactor,
            double maximumDrawdown,
            double maximumRunup,
            double maxFavorableExcursion,
            double averageFavorableExcursion,
            double maxAdverseExcursion,
            double averageAdverseExcursion
    ) {
        if (totalTrades < 0 || winningTrades < 0 || losingTrades < 0) {
            throw new IllegalArgumentException("trade counts cannot be negative");
        }
        this.totalTrades = totalTrades;
        this.winningTrades = winningTrades;
        this.losingTrades = losingTrades;
        this.winRate = winRate;
        this.netProfitLoss = netProfitLoss;
        this.grossProfit = grossProfit;
        this.grossLoss = grossLoss;
        this.commissions = commissions;
        this.averageTrade = averageTrade;
        this.averageWinningTrade = averageWinningTrade;
        this.averageLosingTrade = averageLosingTrade;
        this.profitFactor = profitFactor;
        this.maximumDrawdown = maximumDrawdown;
        this.maximumRunup = maximumRunup;
        this.maxFavorableExcursion = maxFavorableExcursion;
        this.averageFavorableExcursion = averageFavorableExcursion;
        this.maxAdverseExcursion = maxAdverseExcursion;
        this.averageAdverseExcursion = averageAdverseExcursion;
    }

    public static PerformanceMetrics fromTrades(List<TradeResult> trades) {
        if (trades == null || trades.isEmpty()) {
            return new PerformanceMetrics();
        }

        int winningTrades = 0;
        int losingTrades = 0;
        double grossProfit = 0;
        double grossLoss = 0;
        double equity = 0;
        double peakEquity = 0;
        double troughEquity = 0;
        double maximumDrawdown = 0;
        double maximumRunup = 0;
        double favorableExcursionTotal = 0;
        double adverseExcursionTotal = 0;
        double maxFavorableExcursion = 0;
        double maxAdverseExcursion = 0;

        for (TradeResult trade : trades) {
            double tradeProfit = trade.getGrossDollars();
            if (tradeProfit > 0) {
                winningTrades++;
                grossProfit += tradeProfit;
            } else if (tradeProfit < 0) {
                losingTrades++;
                grossLoss += tradeProfit;
            }

            equity += tradeProfit;
            if (equity > peakEquity) {
                peakEquity = equity;
            }
            if (equity < troughEquity) {
                troughEquity = equity;
            }
            maximumDrawdown = Math.min(maximumDrawdown, equity - peakEquity);
            maximumRunup = Math.max(maximumRunup, equity - troughEquity);

            favorableExcursionTotal += trade.getMaxFavorableExcursionDollars();
            adverseExcursionTotal += trade.getMaxAdverseExcursionDollars();
            maxFavorableExcursion = Math.max(maxFavorableExcursion, trade.getMaxFavorableExcursionDollars());
            maxAdverseExcursion = Math.min(maxAdverseExcursion, trade.getMaxAdverseExcursionDollars());
        }

        int totalTrades = trades.size();
        double netProfitLoss = grossProfit + grossLoss;
        return new PerformanceMetrics(
                totalTrades,
                winningTrades,
                losingTrades,
                percentage(winningTrades, totalTrades),
                netProfitLoss,
                grossProfit,
                grossLoss,
                0,
                netProfitLoss / totalTrades,
                winningTrades == 0 ? 0 : grossProfit / winningTrades,
                losingTrades == 0 ? 0 : grossLoss / losingTrades,
                grossLoss == 0 ? (grossProfit > 0 ? Double.POSITIVE_INFINITY : 0) : grossProfit / Math.abs(grossLoss),
                maximumDrawdown,
                maximumRunup,
                maxFavorableExcursion,
                favorableExcursionTotal / totalTrades,
                maxAdverseExcursion,
                adverseExcursionTotal / totalTrades
        );
    }

    public int getTotalTrades() {
        return totalTrades;
    }

    public int getWinningTrades() {
        return winningTrades;
    }

    public int getLosingTrades() {
        return losingTrades;
    }

    public double getWinRate() {
        return winRate;
    }

    public double getNetProfitLoss() {
        return netProfitLoss;
    }

    public double getGrossProfit() {
        return grossProfit;
    }

    public double getGrossLoss() {
        return grossLoss;
    }

    public double getCommissions() {
        return commissions;
    }

    public double getAverageTrade() {
        return averageTrade;
    }

    public double getAverageWinningTrade() {
        return averageWinningTrade;
    }

    public double getAverageLosingTrade() {
        return averageLosingTrade;
    }

    public double getProfitFactor() {
        return profitFactor;
    }

    public double getMaximumDrawdown() {
        return maximumDrawdown;
    }

    public double getMaximumRunup() {
        return maximumRunup;
    }

    public double getMaxFavorableExcursion() {
        return maxFavorableExcursion;
    }

    public double getAverageFavorableExcursion() {
        return averageFavorableExcursion;
    }

    public double getMaxAdverseExcursion() {
        return maxAdverseExcursion;
    }

    public double getAverageAdverseExcursion() {
        return averageAdverseExcursion;
    }

    private static double percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return 0;
        }
        return ((double) numerator / denominator) * 100;
    }
}
