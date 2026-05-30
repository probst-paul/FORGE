package forge.data.rollover;

import forge.data.contract.FuturesContractCode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.Set;

public class EquityIndexRolloverRule implements RolloverRule {
    private static final Set<String> EQUITY_INDEX_ROOTS = Set.of("ES", "NQ", "YM", "RTY");

    @Override
    public Optional<ContractRolloverWindow> resolveActiveWindow(FuturesContractCode contractCode) {
        /*
         * Intent: Resolve the active front-month window for quarterly CME equity index futures.
         * Precondition: Contract code must be non-null.
         * Returns: Active window for supported quarterly roots/months, otherwise Optional.empty().
         * Postcondition: Rule state is unchanged.
         */
        if (contractCode == null) {
            throw new IllegalArgumentException("contractCode is required");
        }
        if (!EQUITY_INDEX_ROOTS.contains(contractCode.getInstrumentSymbol())) {
            return Optional.empty();
        }
        if (!isQuarterlyMonth(contractCode.getMonth())) {
            return Optional.empty();
        }

        FuturesContractCode previousContract = previousQuarterlyContract(contractCode);
        LocalDate activeStartDate = rolloverDate(previousContract);
        LocalDate activeEndDate = rolloverDate(contractCode).minusDays(1);
        return Optional.of(new ContractRolloverWindow(
                contractCode.toContractSymbol(),
                activeStartDate,
                activeEndDate
        ));
    }

    private LocalDate rolloverDate(FuturesContractCode contractCode) {
        /*
         * Intent: Calculate the Monday before the third Friday of the contract month.
         * Precondition: Contract code must represent a quarterly equity index contract.
         * Returns: Rollover date for the supplied contract.
         * Postcondition: Contract code is unchanged.
         */
        LocalDate thirdFriday = LocalDate
                .of(contractCode.getYear(), contractCode.getMonth(), 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.FRIDAY));
        return thirdFriday.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
    }

    private FuturesContractCode previousQuarterlyContract(FuturesContractCode contractCode) {
        /*
         * Intent: Identify the prior quarterly contract used to define this contract's active start date.
         * Precondition: Contract month must be March, June, September, or December.
         * Returns: FuturesContractCode for the previous quarterly expiration.
         * Postcondition: Source contract code is unchanged.
         */
        switch (contractCode.getMonth()) {
            case MARCH:
                return new FuturesContractCode(contractCode.getInstrumentSymbol(), "Z", contractCode.getYear() - 1);
            case JUNE:
                return new FuturesContractCode(contractCode.getInstrumentSymbol(), "H", contractCode.getYear());
            case SEPTEMBER:
                return new FuturesContractCode(contractCode.getInstrumentSymbol(), "M", contractCode.getYear());
            case DECEMBER:
                return new FuturesContractCode(contractCode.getInstrumentSymbol(), "U", contractCode.getYear());
            default:
                throw new IllegalArgumentException("Not a quarterly equity index contract month: " + contractCode.getMonth());
        }
    }

    private boolean isQuarterlyMonth(Month month) {
        return month == Month.MARCH
                || month == Month.JUNE
                || month == Month.SEPTEMBER
                || month == Month.DECEMBER;
    }
}
