package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.data.FacadeForgeData;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.catalog.InstrumentDataCatalog.AvailableInstrumentData;
import forge.data.market.ContractTradeWindow;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstrumentSelectionService {
    private final FacadeForgeData facadeData;

    public InstrumentSelectionService(FacadeForgeData facadeData) {
        /*
         * Intent: Create a CLI selection service backed by the data facade.
         * Precondition: Data facade should be configured and available.
         * Returns: A constructed InstrumentSelectionService instance.
         * Postcondition: Future selections query available instruments/contracts through the supplied facade.
         */
        this.facadeData = facadeData;
    }

    /*
     * Intent: Let the user choose either all available contracts for an instrument or specific contract windows.
     * Precondition: Imported contract metadata must exist and include at least one valid front-month window.
     * Returns: SelectedBacktestContracts containing the chosen contract windows.
     * Postcondition: Input is consumed until a valid selection is made or the user quits.
     */
    public SelectedBacktestContracts selectContracts(UserInput input, UserOutput output) {
        List<AvailableInstrumentData> instruments = facadeData.forgeDataAccess().getAvailableInstruments();
        List<AvailableContractData> contracts = facadeData.forgeDataAccess().getAvailableContracts();
        if (instruments.isEmpty()) {
            throw new IllegalStateException("No imported instruments are available. Import SCID data first, then run this action again");
        }
        if (contracts.isEmpty()) {
            throw new IllegalStateException("No valid front-month contract windows are available. Import contract data that overlaps a supported rollover window");
        }

        output.printLine("Available instruments:");
        for (int i = 0; i < instruments.size(); i++) {
            output.printLine((i + 1) + ". " + instruments.get(i).getSymbol() + " - All Available");
        }
        int customContractsOption = instruments.size() + 1;
        output.printLine(customContractsOption + ". Select Custom Contracts");

        while (true) {
            int selectedIndex = input.readInt("Select instrument option");
            if (selectedIndex >= 1 && selectedIndex <= instruments.size()) {
                String selectedInstrumentSymbol = instruments.get(selectedIndex - 1).getSymbol();
                List<AvailableContractData> selectedContracts = contracts.stream()
                        .filter(contract -> contract.getInstrumentSymbol().equals(selectedInstrumentSymbol))
                        .collect(Collectors.toList());
                if (selectedContracts.isEmpty()) {
                    throw new IllegalStateException("No valid front-month contract windows are available for " + selectedInstrumentSymbol);
                }
                return toSelectedBacktestContracts(selectedContracts);
            }
            if (selectedIndex == customContractsOption) {
                return selectCustomContracts(input, output, contracts);
            }
            output.printLine("Selected instrument option is not available. Please select an available option, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Provide legacy/simple instrument selection as contract-symbol output.
     * Precondition: selectContracts preconditions must be satisfied.
     * Returns: List of selected contract symbols.
     * Postcondition: Selection is still based on rollover-valid contract windows.
     */
    public List<String> selectInstruments(UserInput input, UserOutput output) {
        return selectContracts(input, output).getContractSymbols();
    }

    /*
     * Intent: Let the user choose one or more specific contract windows by comma-separated menu number.
     * Precondition: Contracts list must contain available front-month contract windows.
     * Returns: SelectedBacktestContracts for the chosen contract windows.
     * Postcondition: Invalid selections are rejected and reprompted without changing data state.
     */
    private SelectedBacktestContracts selectCustomContracts(
            UserInput input,
            UserOutput output,
            List<AvailableContractData> contracts
    ) {
        output.printLine("Available contracts:");
        for (int i = 0; i < contracts.size(); i++) {
            output.printLine((i + 1) + ". " + contracts.get(i));
        }

        while (true) {
            String rawSelections = input.readString("Select contracts (comma separated numbers)");
            try {
                List<AvailableContractData> selectedContracts = Arrays.stream(rawSelections.split(","))
                        .map(String::trim)
                        .filter(selection -> !selection.isEmpty())
                        .map(selection -> {
                            int selectedIndex = Integer.parseInt(selection) - 1;
                            if (selectedIndex < 0 || selectedIndex >= contracts.size()) {
                                throw new IllegalArgumentException("Selected contract is not available");
                            }
                            return contracts.get(selectedIndex);
                        })
                        .collect(Collectors.toList());
                if (selectedContracts.isEmpty()) {
                    throw new IllegalArgumentException("At least one contract must be selected");
                }
                return toSelectedBacktestContracts(selectedContracts);
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter available contract numbers, or enter 'quit' to exit program.");
            }
        }
    }

    /*
     * Intent: Convert catalog contract metadata into the backtest contract-window selection model.
     * Precondition: Contracts list must contain at least one non-null available contract.
     * Returns: SelectedBacktestContracts with ContractTradeWindow entries.
     * Postcondition: Source catalog objects are not modified.
     */
    private SelectedBacktestContracts toSelectedBacktestContracts(List<AvailableContractData> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            throw new IllegalArgumentException("at least one contract must be selected");
        }
        List<ContractTradeWindow> contractWindows = new ArrayList<>();
        for (AvailableContractData contract : contracts) {
            contractWindows.add(new ContractTradeWindow(
                    contract.getContractSymbol(),
                    contract.getStartDate(),
                    contract.getEndDate()
            ));
        }
        return new SelectedBacktestContracts(contractWindows);
    }
}
