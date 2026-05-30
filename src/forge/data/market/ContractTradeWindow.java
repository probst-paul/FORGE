package forge.data.market;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class ContractTradeWindow {
    private final String contractSymbol;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public ContractTradeWindow(String contractSymbol, LocalDate startDate, LocalDate endDate) {
        /*
         * Intent: Represent one selected contract's inclusive date window for reads/backtests.
         * Precondition: Contract symbol and dates must be valid and ordered.
         * Returns: A constructed ContractTradeWindow instance.
         * Postcondition: Contract symbol is normalized to uppercase and dates are immutable.
         */
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

    public Instant getStartInclusiveInstant() {
        /*
         * Intent: Convert the window's start date into the inclusive UTC query boundary.
         * Precondition: Start date must be present.
         * Returns: UTC instant at start of the start date.
         * Postcondition: Window state is unchanged.
         */
        return startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public Instant getEndExclusiveInstant() {
        /*
         * Intent: Convert the inclusive end date into the exclusive UTC query boundary.
         * Precondition: End date must be present.
         * Returns: UTC instant at start of the day after the end date.
         * Postcondition: Window state is unchanged.
         */
        return endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    @Override
    public String toString() {
        return contractSymbol + ": " + startDate + " to " + endDate;
    }
}
