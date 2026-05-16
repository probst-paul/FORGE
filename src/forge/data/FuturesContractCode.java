package forge.data;

import java.time.Month;

public class FuturesContractCode {
    private final String instrumentSymbol;
    private final String monthCode;
    private final int year;

    public FuturesContractCode(String instrumentSymbol, String monthCode, int year) {
        if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("instrumentSymbol is required");
        }
        if (monthCode == null || monthCode.trim().isEmpty()) {
            throw new IllegalArgumentException("monthCode is required");
        }
        if (year < 2000) {
            throw new IllegalArgumentException("year must include century");
        }
        this.instrumentSymbol = instrumentSymbol.trim().toUpperCase();
        this.monthCode = monthCode.trim().toUpperCase();
        this.year = year;
    }

    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }

    public String getMonthCode() {
        return monthCode;
    }

    public int getYear() {
        return year;
    }

    public Month getMonth() {
        switch (monthCode) {
            case "F":
                return Month.JANUARY;
            case "G":
                return Month.FEBRUARY;
            case "H":
                return Month.MARCH;
            case "J":
                return Month.APRIL;
            case "K":
                return Month.MAY;
            case "M":
                return Month.JUNE;
            case "N":
                return Month.JULY;
            case "Q":
                return Month.AUGUST;
            case "U":
                return Month.SEPTEMBER;
            case "V":
                return Month.OCTOBER;
            case "X":
                return Month.NOVEMBER;
            case "Z":
                return Month.DECEMBER;
            default:
                throw new IllegalArgumentException("Unsupported futures month code: " + monthCode);
        }
    }

    public String toContractSymbol() {
        return instrumentSymbol + monthCode + String.format("%02d", year % 100);
    }
}
