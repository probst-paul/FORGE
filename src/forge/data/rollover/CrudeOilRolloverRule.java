package forge.data.rollover;

import forge.data.contract.FuturesContractCode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

public class CrudeOilRolloverRule implements RolloverRule {
    @Override
    public Optional<ContractRolloverWindow> resolveActiveWindow(FuturesContractCode contractCode) {
        /*
         * Intent: Resolve the active front-month window for CL monthly futures.
         * Precondition: Contract code must be non-null.
         * Returns: Active window for CL contracts, otherwise Optional.empty().
         * Postcondition: Rule state is unchanged.
         */
        if (contractCode == null) {
            throw new IllegalArgumentException("contractCode is required");
        }
        if (!"CL".equals(contractCode.getInstrumentSymbol())) {
            return Optional.empty();
        }

        FuturesContractCode previousContract = previousMonthlyContract(contractCode);
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
         * Intent: Calculate CL rollover as the Friday before the estimated expiration date.
         * Precondition: Contract code must represent a CL monthly contract.
         * Returns: Rollover date for the contract.
         * Postcondition: Contract code is unchanged.
         */
        return expirationDate(contractCode).with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
    }

    private LocalDate expirationDate(FuturesContractCode contractCode) {
        /*
         * Intent: Estimate CL expiration as three business days before the 25th of the prior month.
         * Precondition: Contract code must contain a valid delivery month and year.
         * Returns: Estimated expiration date used by the rollover rule.
         * Postcondition: Contract code is unchanged.
         */
        LocalDate twentyFifthOfPreviousMonth = LocalDate
                .of(contractCode.getYear(), contractCode.getMonth(), 1)
                .minusMonths(1)
                .withDayOfMonth(25);
        return subtractBusinessDays(twentyFifthOfPreviousMonth, 3);
    }

    private LocalDate subtractBusinessDays(LocalDate date, int businessDays) {
        /*
         * Intent: Move backward a fixed number of weekdays from a date.
         * Precondition: Date must be non-null and business day count must be nonnegative.
         * Returns: Date after subtracting the requested number of business days.
         * Postcondition: Input date is unchanged.
         */
        LocalDate currentDate = date;
        int remainingBusinessDays = businessDays;
        while (remainingBusinessDays > 0) {
            currentDate = currentDate.minusDays(1);
            if (isBusinessDay(currentDate)) {
                remainingBusinessDays--;
            }
        }
        return currentDate;
    }

    private boolean isBusinessDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private FuturesContractCode previousMonthlyContract(FuturesContractCode contractCode) {
        /*
         * Intent: Identify the prior monthly CL contract used to define active start date.
         * Precondition: Contract code must contain a valid month and year.
         * Returns: FuturesContractCode for the previous calendar month.
         * Postcondition: Source contract code is unchanged.
         */
        Month previousMonth = contractCode.getMonth().minus(1);
        int previousYear = contractCode.getMonth() == Month.JANUARY
                ? contractCode.getYear() - 1
                : contractCode.getYear();
        return new FuturesContractCode(
                contractCode.getInstrumentSymbol(),
                monthCode(previousMonth),
                previousYear
        );
    }

    private String monthCode(Month month) {
        switch (month) {
            case JANUARY:
                return "F";
            case FEBRUARY:
                return "G";
            case MARCH:
                return "H";
            case APRIL:
                return "J";
            case MAY:
                return "K";
            case JUNE:
                return "M";
            case JULY:
                return "N";
            case AUGUST:
                return "Q";
            case SEPTEMBER:
                return "U";
            case OCTOBER:
                return "V";
            case NOVEMBER:
                return "X";
            case DECEMBER:
                return "Z";
            default:
                throw new IllegalArgumentException("Unsupported futures month: " + month);
        }
    }
}
