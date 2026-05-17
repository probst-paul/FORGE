package forge.cli;

import forge.data.market.ContractTradeWindow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectedBacktestContracts {
    private final List<String> contractSymbols;
    private final List<ContractTradeWindow> contractWindows;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public SelectedBacktestContracts(List<String> contractSymbols, LocalDate startDate, LocalDate endDate) {
        this(toContractWindows(contractSymbols, startDate, endDate));
    }

    public SelectedBacktestContracts(List<ContractTradeWindow> contractWindows) {
        if (contractWindows == null || contractWindows.isEmpty()) {
            throw new IllegalArgumentException("at least one contract must be selected");
        }
        List<String> normalizedContractSymbols = new ArrayList<>();
        List<ContractTradeWindow> normalizedContractWindows = new ArrayList<>();
        LocalDate earliestStartDate = null;
        LocalDate latestEndDate = null;
        for (ContractTradeWindow contractWindow : contractWindows) {
            if (contractWindow == null) {
                throw new IllegalArgumentException("contract windows cannot contain null values");
            }
            normalizedContractWindows.add(contractWindow);
            normalizedContractSymbols.add(contractWindow.getContractSymbol());
            if (earliestStartDate == null || contractWindow.getStartDate().isBefore(earliestStartDate)) {
                earliestStartDate = contractWindow.getStartDate();
            }
            if (latestEndDate == null || contractWindow.getEndDate().isAfter(latestEndDate)) {
                latestEndDate = contractWindow.getEndDate();
            }
        }
        this.contractSymbols = Collections.unmodifiableList(normalizedContractSymbols);
        this.contractWindows = Collections.unmodifiableList(normalizedContractWindows);
        this.startDate = earliestStartDate;
        this.endDate = latestEndDate;
    }

    public List<String> getContractSymbols() {
        return contractSymbols;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    private static List<ContractTradeWindow> toContractWindows(
            List<String> contractSymbols,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (contractSymbols == null || contractSymbols.isEmpty()) {
            throw new IllegalArgumentException("at least one contract must be selected");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("endDate is required");
        }
        List<ContractTradeWindow> windows = new ArrayList<>();
        for (String contractSymbol : contractSymbols) {
            windows.add(new ContractTradeWindow(contractSymbol, startDate, endDate));
        }
        return windows;
    }
}
