package forge.data.catalog;

import java.time.LocalDate;

public class ContractDataSummary {
    private final String contractSymbol;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public ContractDataSummary(String contractSymbol, LocalDate startDate, LocalDate endDate) {
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
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
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
