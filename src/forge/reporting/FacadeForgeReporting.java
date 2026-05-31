package forge.reporting;

import forge.engine.backtest.BacktestResult;
import forge.engine.backtest.ContractBacktestResult;
import forge.engine.backtest.InstrumentBacktestResult;
import forge.reporting.backtest.BacktestReport;
import forge.reporting.backtest.ContractPerformanceReport;
import forge.reporting.backtest.InstrumentPerformanceReport;
import forge.reporting.backtest.TradePerformanceReport;
import forge.trade.TradeResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FacadeForgeReporting {
    private static final FacadeForgeReporting THE_INSTANCE = new FacadeForgeReporting();

    private final ForgeReportingAccess access = new ForgeReportingAccess();

    public static FacadeForgeReporting getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeReportingAccess forgeReportingAccess() {
        return access;
    }

    public static class ForgeReportingAccess {
        /*
         * Intent: Convert an engine backtest result into a display/export-ready report model.
         * Precondition: result must be non-null and contain valid nested result objects.
         * Returns: BacktestReport with instrument, contract, and trade-row report sections.
         * Postcondition: Source result objects are not modified.
         */
        public BacktestReport buildBacktestReport(BacktestResult result) {
            BacktestResult source = Objects.requireNonNull(result, "result is required");
            List<InstrumentPerformanceReport> instrumentReports = new ArrayList<>();
            for (InstrumentBacktestResult instrumentResult : source.getInstrumentResults()) {
                instrumentReports.add(buildInstrumentReport(instrumentResult));
            }
            return new BacktestReport(
                    source.getStrategyName(),
                    source.getContractSymbols(),
                    source.getTicksProcessed(),
                    source.getOrderSignalsGenerated(),
                    instrumentReports
            );
        }

        /*
         * Intent: Convert a backtest result into report text.
         * Precondition: result must be non-null.
         * Returns: Multi-line summary generated from a report model.
         * Postcondition: Result state is unchanged.
         */
        public String summarize(BacktestResult result) {
            return summarize(buildBacktestReport(result));
        }

        /*
         * Intent: Convert a report model into CLI/export-friendly summary text.
         * Precondition: report must be non-null.
         * Returns: Multi-line summary of run totals and performance sections.
         * Postcondition: Report state is unchanged.
         */
        public String summarize(BacktestReport report) {
            BacktestReport source = Objects.requireNonNull(report, "report is required");
            StringBuilder builder = new StringBuilder();
            builder.append("Strategy: ").append(source.getStrategyName()).append(System.lineSeparator());
            builder.append("Ticks processed: ").append(source.getTicksProcessed()).append(System.lineSeparator());
            builder.append("Order signals: ").append(source.getOrderSignalsGenerated());
            for (InstrumentPerformanceReport instrumentReport : source.getInstrumentReports()) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
                appendSection(builder, instrumentReport.getInstrumentSymbol() + " Summary", instrumentReport.getPerformanceMetrics());
                for (ContractPerformanceReport contractReport : instrumentReport.getContractReports()) {
                    builder.append(System.lineSeparator()).append(System.lineSeparator());
                    appendSection(builder, contractReport.getContractSymbol(), contractReport.getPerformanceMetrics());
                }
            }
            return builder.toString();
        }

        private InstrumentPerformanceReport buildInstrumentReport(InstrumentBacktestResult instrumentResult) {
            /*
             * Intent: Convert one instrument result into a report section.
             * Precondition: instrumentResult must be non-null.
             * Returns: InstrumentPerformanceReport containing contract report sections.
             * Postcondition: Source result state is unchanged.
             */
            InstrumentBacktestResult source = Objects.requireNonNull(instrumentResult, "instrumentResult is required");
            List<ContractPerformanceReport> contractReports = new ArrayList<>();
            for (ContractBacktestResult contractResult : source.getContractResults()) {
                contractReports.add(buildContractReport(contractResult));
            }
            return new InstrumentPerformanceReport(
                    source.getInstrumentSymbol(),
                    source.getTicksProcessed(),
                    source.getOrderSignalsGenerated(),
                    source.getPerformanceMetrics(),
                    contractReports
            );
        }

        private ContractPerformanceReport buildContractReport(ContractBacktestResult contractResult) {
            /*
             * Intent: Convert one contract result into a report section.
             * Precondition: contractResult must be non-null.
             * Returns: ContractPerformanceReport containing trade report rows.
             * Postcondition: Source result state is unchanged.
             */
            ContractBacktestResult source = Objects.requireNonNull(contractResult, "contractResult is required");
            List<TradePerformanceReport> trades = new ArrayList<>();
            for (TradeResult trade : source.getTrades()) {
                trades.add(new TradePerformanceReport(trade));
            }
            return new ContractPerformanceReport(
                    source.getContractSymbol(),
                    source.getTicksProcessed(),
                    source.getOrderSignalsGenerated(),
                    source.getPerformanceMetrics(),
                    trades
            );
        }

        private void appendSection(StringBuilder builder, String title, PerformanceMetrics metrics) {
            /*
             * Intent: Append one formatted performance section to a text report.
             * Precondition: builder, title, and metrics must be non-null.
             * Returns: Nothing.
             * Postcondition: Builder contains formatted metrics for the requested section.
             */
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
}
