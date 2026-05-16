package forge.cli;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectedBacktestContracts {
    private final List<String> contractSymbols;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public SelectedBacktestContracts(List<String> contractSymbols, LocalDate startDate, LocalDate endDate) {
        if (contractSymbols == null || contractSymbols.isEmpty()) {
            throw new IllegalArgumentException("at least one contract must be selected");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("endDate is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }
        List<String> normalizedContractSymbols = new ArrayList<>();
        for (String contractSymbol : contractSymbols) {
            if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
                throw new IllegalArgumentException("contract symbols cannot be blank");
            }
            normalizedContractSymbols.add(contractSymbol.trim().toUpperCase());
        }
        this.contractSymbols = Collections.unmodifiableList(normalizedContractSymbols);
        this.startDate = startDate;
        this.endDate = endDate;
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
}
