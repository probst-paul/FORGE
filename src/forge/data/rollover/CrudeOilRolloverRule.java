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
        return expirationDate(contractCode).with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
    }

    private LocalDate expirationDate(FuturesContractCode contractCode) {
        LocalDate twentyFifthOfPreviousMonth = LocalDate
                .of(contractCode.getYear(), contractCode.getMonth(), 1)
                .minusMonths(1)
                .withDayOfMonth(25);
        return subtractBusinessDays(twentyFifthOfPreviousMonth, 3);
    }

    private LocalDate subtractBusinessDays(LocalDate date, int businessDays) {
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
