package forge.feature;

import java.time.LocalDate;

public class SessionRangeFeature extends FeatureResult {
    public static final String FEATURE_NAME = "SESSION_RANGE";
    public static final int FEATURE_VERSION = 1;

    private final String contractSymbol;
    private final LocalDate sessionDate;
    private final long overnightLowTicks;
    private final long overnightHighTicks;
    private final long firstHourLowTicks;
    private final long firstHourHighTicks;
    private final long rthLowTicks;
    private final long rthHighTicks;

    public SessionRangeFeature(
            String contractSymbol,
            LocalDate sessionDate,
            long overnightLowTicks,
            long overnightHighTicks,
            long firstHourLowTicks,
            long firstHourHighTicks,
            long rthLowTicks,
            long rthHighTicks
    ) {
        super(FEATURE_NAME, FEATURE_VERSION);
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
        }
        if (sessionDate == null) {
            throw new IllegalArgumentException("sessionDate is required");
        }
        validateRange(overnightLowTicks, overnightHighTicks, "overnight");
        validateRange(firstHourLowTicks, firstHourHighTicks, "firstHour");
        validateRange(rthLowTicks, rthHighTicks, "rth");
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.sessionDate = sessionDate;
        this.overnightLowTicks = overnightLowTicks;
        this.overnightHighTicks = overnightHighTicks;
        this.firstHourLowTicks = firstHourLowTicks;
        this.firstHourHighTicks = firstHourHighTicks;
        this.rthLowTicks = rthLowTicks;
        this.rthHighTicks = rthHighTicks;
    }

    private void validateRange(long lowTicks, long highTicks, String name) {
        if (lowTicks <= 0 || highTicks <= 0) {
            throw new IllegalArgumentException(name + " range prices must be greater than zero");
        }
        if (highTicks < lowTicks) {
            throw new IllegalArgumentException(name + " high ticks must be greater than or equal to low ticks");
        }
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public long getOvernightLowTicks() {
        return overnightLowTicks;
    }

    public long getOvernightHighTicks() {
        return overnightHighTicks;
    }

    public long getFirstHourLowTicks() {
        return firstHourLowTicks;
    }

    public long getFirstHourHighTicks() {
        return firstHourHighTicks;
    }

    public long getRthLowTicks() {
        return rthLowTicks;
    }

    public long getRthHighTicks() {
        return rthHighTicks;
    }
}
