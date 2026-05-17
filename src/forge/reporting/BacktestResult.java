package forge.reporting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BacktestResult {
    private final String strategyName;
    private final List<String> contractSymbols;
    private final long ticksProcessed;
    private final long orderSignalsGenerated;
    private final List<InstrumentBacktestResult> instrumentResults;

    public BacktestResult() {
        this("Not Run", Collections.emptyList());
    }

    public BacktestResult(
            String strategyName,
            List<String> contractSymbols,
            long ticksProcessed,
            long orderSignalsGenerated
    ) {
        this.strategyName = requireStrategyName(strategyName);
        this.contractSymbols = normalizeContractSymbols(contractSymbols);
        if (ticksProcessed < 0) {
            throw new IllegalArgumentException("ticksProcessed cannot be negative");
        }
        if (orderSignalsGenerated < 0) {
            throw new IllegalArgumentException("orderSignalsGenerated cannot be negative");
        }
        this.ticksProcessed = ticksProcessed;
        this.orderSignalsGenerated = orderSignalsGenerated;
        this.instrumentResults = Collections.emptyList();
    }

    public BacktestResult(String strategyName, List<InstrumentBacktestResult> instrumentResults) {
        if (instrumentResults == null) {
            throw new IllegalArgumentException("instrumentResults is required");
        }
        this.strategyName = requireStrategyName(strategyName);
        this.instrumentResults = Collections.unmodifiableList(new ArrayList<>(instrumentResults));
        List<String> symbols = new ArrayList<>();
        long ticks = 0;
        long signals = 0;
        for (InstrumentBacktestResult instrumentResult : instrumentResults) {
            ticks += instrumentResult.getTicksProcessed();
            signals += instrumentResult.getOrderSignalsGenerated();
            for (ContractBacktestResult contractResult : instrumentResult.getContractResults()) {
                symbols.add(contractResult.getContractSymbol());
            }
        }
        this.contractSymbols = Collections.unmodifiableList(symbols);
        this.ticksProcessed = ticks;
        this.orderSignalsGenerated = signals;
    }

    private String requireStrategyName(String strategyName) {
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new IllegalArgumentException("strategyName is required");
        }
        return strategyName.trim();
    }

    private List<String> normalizeContractSymbols(List<String> contractSymbols) {
        if (contractSymbols == null) {
            throw new IllegalArgumentException("contractSymbols is required");
        }
        List<String> normalizedContractSymbols = new ArrayList<>();
        for (String contractSymbol : contractSymbols) {
            if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
                throw new IllegalArgumentException("contract symbols cannot be blank");
            }
            normalizedContractSymbols.add(contractSymbol.trim().toUpperCase());
        }
        return Collections.unmodifiableList(normalizedContractSymbols);
    }

    public String getStrategyName() {
        return strategyName;
    }

    public List<String> getContractSymbols() {
        return contractSymbols;
    }

    public long getTicksProcessed() {
        return ticksProcessed;
    }

    public long getOrderSignalsGenerated() {
        return orderSignalsGenerated;
    }

    public List<InstrumentBacktestResult> getInstrumentResults() {
        return instrumentResults;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Strategy: ").append(strategyName).append(System.lineSeparator());
        builder.append("Ticks processed: ").append(ticksProcessed).append(System.lineSeparator());
        builder.append("Order signals: ").append(orderSignalsGenerated);
        for (InstrumentBacktestResult instrumentResult : instrumentResults) {
            builder.append(System.lineSeparator()).append(System.lineSeparator());
            appendSection(builder, instrumentResult.getInstrumentSymbol() + " Summary", instrumentResult.getPerformanceMetrics());
            for (ContractBacktestResult contractResult : instrumentResult.getContractResults()) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
                appendSection(builder, contractResult.getContractSymbol(), contractResult.getPerformanceMetrics());
            }
        }
        return builder.toString();
    }

    private void appendSection(StringBuilder builder, String title, PerformanceMetrics metrics) {
        builder.append(title).append(System.lineSeparator());
        builder.append("-------------------------").append(System.lineSeparator());
        builder.append("Total Trades: ").append(metrics.getTotalTrades()).append(System.lineSeparator());
        builder.append("Wins/Losses: ").append(metrics.getWinningTrades()).append(" / ").append(metrics.getLosingTrades()).append(System.lineSeparator());
        builder.append(String.format("Win Rate: %.2f%%%n", metrics.getWinRate()));
        builder.append(String.format("Net Profit/Loss: $%.2f%n", metrics.getNetProfitLoss()));
        builder.append(String.format("Gross Profit: $%.2f%n", metrics.getGrossProfit()));
        builder.append(String.format("Gross Loss: $%.2f%n", metrics.getGrossLoss()));
        builder.append(String.format("Commissions: $%.2f%n", metrics.getCommissions()));
        builder.append(String.format("Average Trade: $%.2f%n", metrics.getAverageTrade()));
        builder.append(String.format("Average Winning Trade: $%.2f%n", metrics.getAverageWinningTrade()));
        builder.append(String.format("Average Losing Trade: $%.2f%n", metrics.getAverageLosingTrade()));
        builder.append(String.format("Profit Factor: %.2f%n", metrics.getProfitFactor()));
        builder.append(String.format("Maximum Drawdown: $%.2f%n", metrics.getMaximumDrawdown()));
        builder.append(String.format("Maximum Runup: $%.2f%n", metrics.getMaximumRunup()));
        builder.append(String.format("Max Favorable Excursion: $%.2f%n", metrics.getMaxFavorableExcursion()));
        builder.append(String.format("Avg Favorable Excursion: $%.2f%n", metrics.getAverageFavorableExcursion()));
        builder.append(String.format("Max Adverse Excursion: $%.2f%n", metrics.getMaxAdverseExcursion()));
        builder.append(String.format("Avg Adverse Excursion: $%.2f", metrics.getAverageAdverseExcursion()));
    }
}
