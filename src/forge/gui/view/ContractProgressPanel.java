package forge.gui.view;

import forge.data.market.ContractTradeWindow;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ContractProgressPanel {
    private final VBox rows;
    private final Map<String, ContractProgressRow> rowsByContract = new LinkedHashMap<>();

    ContractProgressPanel(VBox rows) {
        if (rows == null) {
            throw new IllegalArgumentException("rows is required");
        }
        this.rows = rows;
        collapse();
    }

    void configure(List<ContractTradeWindow> windows) {
        /*
         * Intent: Prepare one temporary progress row for each selected contract in a multi-contract run.
         * Precondition: Windows must represent the pending event-statistics or backtest run.
         * Returns: Nothing.
         * Postcondition: Rows are visible only when more than one contract was selected.
         */
        runOnFxThread(() -> {
            rows.getChildren().clear();
            rowsByContract.clear();
            if (windows == null || windows.size() <= 1) {
                collapse();
                return;
            }
            Label heading = new Label("Contract progress");
            heading.setStyle("-fx-font-weight: bold;");
            rows.getChildren().add(heading);
            for (ContractTradeWindow window : windows) {
                if (!rowsByContract.containsKey(window.getContractSymbol())) {
                    ContractProgressRow row = new ContractProgressRow(window.getContractSymbol());
                    rowsByContract.put(window.getContractSymbol(), row);
                    rows.getChildren().add(row.container());
                }
            }
            rows.setVisible(true);
            rows.setManaged(true);
        });
    }

    void update(String contractSymbol, long processed, long total) {
        /*
         * Intent: Update one contract's temporary progress row.
         * Precondition: Contract symbol should match a configured row; counts must be nonnegative.
         * Returns: Nothing.
         * Postcondition: Matching row shows only that contract's processed/total progress.
         */
        runOnFxThread(() -> {
            ContractProgressRow row = rowsByContract.get(contractSymbol);
            if (row == null) {
                return;
            }
            row.update(processed, total);
        });
    }

    void collapse() {
        /*
         * Intent: Hide per-contract rows once aggregate progress has completed or no multi-contract run is active.
         * Precondition: May be called before rows are configured.
         * Returns: Nothing.
         * Postcondition: Only the aggregate progress bar remains visible.
         */
        runOnFxThread(() -> {
            rows.setVisible(false);
            rows.setManaged(false);
            rows.getChildren().clear();
            rowsByContract.clear();
        });
    }

    private void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private static class ContractProgressRow {
        private final HBox container;
        private final ProgressBar progressBar;
        private final Label details;

        private ContractProgressRow(String contractSymbol) {
            Label label = new Label(contractSymbol);
            label.setMinWidth(72);
            label.setPrefWidth(72);

            progressBar = new ProgressBar(0);
            progressBar.setMinWidth(220);
            progressBar.setMaxWidth(Double.MAX_VALUE);

            details = new Label("0%  0/0 ticks");
            details.setMinWidth(150);
            details.setPrefWidth(150);
            details.setMaxWidth(150);
            details.setAlignment(Pos.CENTER_RIGHT);

            container = new HBox(10, label, progressBar, details);
            container.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(progressBar, Priority.ALWAYS);
        }

        private HBox container() {
            return container;
        }

        private void update(long processed, long total) {
            long safeTotal = Math.max(0, total);
            long safeProcessed = Math.max(0, Math.min(processed, safeTotal));
            double ratio = safeTotal == 0 ? 1.0 : (double) safeProcessed / safeTotal;
            progressBar.setProgress(ratio);
            details.setText(String.format("%.0f%%  %d/%d ticks", ratio * 100.0, safeProcessed, safeTotal));
        }
    }
}
