package forge.data;

import java.time.LocalDate;

public class ContractRolloverWindow {
    private final String contractSymbol;
    private final LocalDate activeStartDate;
    private final LocalDate activeEndDate;

    public ContractRolloverWindow(String contractSymbol, LocalDate activeStartDate, LocalDate activeEndDate) {
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
        }
        if (activeStartDate == null) {
            throw new IllegalArgumentException("activeStartDate is required");
        }
        if (activeEndDate == null) {
            throw new IllegalArgumentException("activeEndDate is required");
        }
        if (activeEndDate.isBefore(activeStartDate)) {
            throw new IllegalArgumentException("activeEndDate cannot be before activeStartDate");
        }
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.activeStartDate = activeStartDate;
        this.activeEndDate = activeEndDate;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public LocalDate getActiveStartDate() {
        return activeStartDate;
    }

    public LocalDate getActiveEndDate() {
        return activeEndDate;
    }
}
