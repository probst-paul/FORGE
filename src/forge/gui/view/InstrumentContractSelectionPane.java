package forge.gui.view;

import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.market.ContractTradeWindow;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class InstrumentContractSelectionPane extends VBox {
    private final List<ContractSelection> contractSelections = new ArrayList<>();

    InstrumentContractSelectionPane() {
        super(8);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #d7dde3;");
    }

    void loadContracts(List<AvailableContractData> contracts) {
        /*
         * Intent: Render available contracts grouped below their instrument.
         * Precondition: contracts may be null/empty and should contain imported contract windows.
         * Returns: Nothing.
         * Postcondition: Selection state is reset to all available contracts selected.
         */
        getChildren().clear();
        contractSelections.clear();

        if (contracts == null || contracts.isEmpty()) {
            getChildren().add(new Label("No imported contract windows are available."));
            return;
        }

        Map<String, List<AvailableContractData>> contractsByInstrument = groupByInstrument(contracts);
        for (Map.Entry<String, List<AvailableContractData>> entry : contractsByInstrument.entrySet()) {
            getChildren().add(createInstrumentSection(entry.getKey(), entry.getValue()));
        }
    }

    List<ContractTradeWindow> selectedWindows() {
        /*
         * Intent: Convert checked contract rows into engine-ready contract windows.
         * Precondition: Pane has been loaded with available contracts.
         * Returns: Selected contract windows in visible order.
         * Postcondition: UI selection state is unchanged.
         */
        List<ContractTradeWindow> selectedWindows = new ArrayList<>();
        for (ContractSelection selection : contractSelections) {
            if (selection.checkBox().isSelected()) {
                selectedWindows.add(selection.window());
            }
        }
        return selectedWindows;
    }

    private TitledPane createInstrumentSection(String instrumentSymbol, List<AvailableContractData> contracts) {
        /*
         * Intent: Build one expandable UI section for a single instrument's contract windows.
         * Precondition: instrumentSymbol identifies the grouped contracts and contracts must be non-null.
         * Returns: TitledPane containing selected-by-default contract checkboxes.
         * Postcondition: Contract selections are registered for later request building.
         */
        VBox contractRows = new VBox(6);
        contractRows.setPadding(new Insets(6, 0, 0, 0));
        for (AvailableContractData contract : contracts) {
            ContractTradeWindow window = new ContractTradeWindow(
                    contract.getContractSymbol(),
                    contract.getStartDate(),
                    contract.getEndDate()
            );
            CheckBox checkBox = new CheckBox(contractLabel(contract));
            checkBox.setSelected(true);
            contractSelections.add(new ContractSelection(checkBox, window));
            contractRows.getChildren().add(checkBox);
        }

        TitledPane titledPane = new TitledPane(instrumentSymbol, contractRows);
        titledPane.setExpanded(true);
        titledPane.setCollapsible(true);
        return titledPane;
    }

    private Map<String, List<AvailableContractData>> groupByInstrument(List<AvailableContractData> contracts) {
        /*
         * Intent: Preserve visible instrument order while grouping contract windows.
         * Precondition: contracts must be non-null and ordered as the catalog should display them.
         * Returns: Instrument symbol to ordered contract list map.
         * Postcondition: Source contract list is unchanged.
         */
        Map<String, List<AvailableContractData>> contractsByInstrument = new LinkedHashMap<>();
        for (AvailableContractData contract : contracts) {
            contractsByInstrument
                    .computeIfAbsent(contract.getInstrumentSymbol(), ignored -> new ArrayList<>())
                    .add(contract);
        }
        return contractsByInstrument;
    }

    private String contractLabel(AvailableContractData contract) {
        /*
         * Intent: Render a compact contract row label without repeating the instrument name.
         * Precondition: contract must contain a symbol and imported date window.
         * Returns: User-facing label for the checkbox row.
         * Postcondition: Contract data is unchanged.
         */
        return contractCode(contract) + ": " + contract.getStartDate() + " to " + contract.getEndDate();
    }

    private String contractCode(AvailableContractData contract) {
        /*
         * Intent: Strip the redundant instrument prefix from a contract symbol for GUI display.
         * Precondition: contract must expose instrument and contract symbols.
         * Returns: Contract code such as U25 when possible, otherwise the original symbol.
         * Postcondition: Contract data is unchanged.
         */
        String instrumentSymbol = contract.getInstrumentSymbol();
        String contractSymbol = contract.getContractSymbol();
        if (contractSymbol.startsWith(instrumentSymbol)) {
            return contractSymbol.substring(instrumentSymbol.length());
        }
        return contractSymbol;
    }

    private record ContractSelection(CheckBox checkBox, ContractTradeWindow window) {
    }
}
