package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.data.FacadeForgeData;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.catalog.InstrumentDataCatalog.AvailableInstrumentData;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstrumentSelectionService {
    private final FacadeForgeData facadeData;

    public InstrumentSelectionService(FacadeForgeData facadeData) {
        this.facadeData = facadeData;
    }

    public SelectedBacktestContracts selectContracts(UserInput input, UserOutput output) {
        List<AvailableInstrumentData> instruments = facadeData.forgeDataAccess().getAvailableInstruments();
        List<AvailableContractData> contracts = facadeData.forgeDataAccess().getAvailableContracts();
        if (instruments.isEmpty()) {
            throw new IllegalStateException("No instrument data is available");
        }
        if (contracts.isEmpty()) {
            throw new IllegalStateException("No valid front-month contract data is available");
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
                return toSelectedBacktestContracts(selectedContracts);
            }
            if (selectedIndex == customContractsOption) {
                return selectCustomContracts(input, output, contracts);
            }
            output.printLine("Selected instrument option is not available. Please select an available option, or enter 'quit' to exit program.");
        }
    }

    public List<String> selectInstruments(UserInput input, UserOutput output) {
        return selectContracts(input, output).getContractSymbols();
    }

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

    private SelectedBacktestContracts toSelectedBacktestContracts(List<AvailableContractData> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            throw new IllegalArgumentException("at least one contract must be selected");
        }
        List<String> contractSymbols = new ArrayList<>();
        LocalDate startDate = null;
        LocalDate endDate = null;
        for (AvailableContractData contract : contracts) {
            contractSymbols.add(contract.getContractSymbol());
            if (startDate == null || contract.getStartDate().isBefore(startDate)) {
                startDate = contract.getStartDate();
            }
            if (endDate == null || contract.getEndDate().isAfter(endDate)) {
                endDate = contract.getEndDate();
            }
        }
        return new SelectedBacktestContracts(contractSymbols, startDate, endDate);
    }
}
