package forge.feature;

import java.time.LocalDate;

public class SessionRangeFeature extends FeatureResult {
    public static final String FEATURE_NAME = "SESSION_RANGE";
    public static final int FEATURE_VERSION = 2;

    private final String contractSymbol;
    private final LocalDate sessionDate;
    private final long overnightLowTicks;
    private final long overnightHighTicks;
    private final long firstHourLowTicks;
    private final long firstHourHighTicks;
    private final long rthLowTicks;
    private final long rthHighTicks;
    private final long overnightVolume;
    private final long firstHourVolume;
    private final long rthVolume;
    private final long overnightTradeCount;
    private final long firstHourTradeCount;
    private final long rthTradeCount;

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
        this(
                contractSymbol,
                sessionDate,
                overnightLowTicks,
                overnightHighTicks,
                firstHourLowTicks,
                firstHourHighTicks,
                rthLowTicks,
                rthHighTicks,
                0,
                0,
                0,
                0,
                0,
                0
        );
    }

    public SessionRangeFeature(
            String contractSymbol,
            LocalDate sessionDate,
            long overnightLowTicks,
            long overnightHighTicks,
            long firstHourLowTicks,
            long firstHourHighTicks,
            long rthLowTicks,
            long rthHighTicks,
            long overnightVolume,
            long firstHourVolume,
            long rthVolume,
            long overnightTradeCount,
            long firstHourTradeCount,
            long rthTradeCount
    ) {
        /*
         * Intent: Store complete overnight, first-hour, and RTH ranges for one contract trading day.
         * Precondition: Contract/session identity must be valid and all ranges must have positive ordered tick prices.
         * Returns: A constructed SessionRangeFeature instance.
         * Postcondition: Feature is immutable and contract symbol is normalized to uppercase.
         */
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
        validateNonNegative(overnightVolume, "overnightVolume");
        validateNonNegative(firstHourVolume, "firstHourVolume");
        validateNonNegative(rthVolume, "rthVolume");
        validateNonNegative(overnightTradeCount, "overnightTradeCount");
        validateNonNegative(firstHourTradeCount, "firstHourTradeCount");
        validateNonNegative(rthTradeCount, "rthTradeCount");
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.sessionDate = sessionDate;
        this.overnightLowTicks = overnightLowTicks;
        this.overnightHighTicks = overnightHighTicks;
        this.firstHourLowTicks = firstHourLowTicks;
        this.firstHourHighTicks = firstHourHighTicks;
        this.rthLowTicks = rthLowTicks;
        this.rthHighTicks = rthHighTicks;
        this.overnightVolume = overnightVolume;
        this.firstHourVolume = firstHourVolume;
        this.rthVolume = rthVolume;
        this.overnightTradeCount = overnightTradeCount;
        this.firstHourTradeCount = firstHourTradeCount;
        this.rthTradeCount = rthTradeCount;
    }

    private void validateRange(long lowTicks, long highTicks, String name) {
        /*
         * Intent: Validate one session's low/high tick range before storing the feature.
         * Precondition: Low/high ticks should be positive normalized prices.
         * Returns: Nothing.
         * Postcondition: Invalid ranges fail before feature construction completes.
         */
        if (lowTicks <= 0 || highTicks <= 0) {
            throw new IllegalArgumentException(name + " range prices must be greater than zero");
        }
        if (highTicks < lowTicks) {
            throw new IllegalArgumentException(name + " high ticks must be greater than or equal to low ticks");
        }
    }

    private void validateNonNegative(long value, String name) {
        /*
         * Intent: Validate derived session measurements that can legitimately be zero for older cached rows.
         * Precondition: Value has been calculated or loaded from PostgreSQL.
         * Returns: Nothing.
         * Postcondition: Negative measurements are rejected.
         */
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
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

    public long getOvernightVolume() {
        return overnightVolume;
    }

    public long getFirstHourVolume() {
        return firstHourVolume;
    }

    public long getRthVolume() {
        return rthVolume;
    }

    public long getOvernightTradeCount() {
        return overnightTradeCount;
    }

    public long getFirstHourTradeCount() {
        return firstHourTradeCount;
    }

    public long getRthTradeCount() {
        return rthTradeCount;
    }

    public long getOvernightRangeTicks() {
        return overnightHighTicks - overnightLowTicks;
    }

    public long getFirstHourRangeTicks() {
        return firstHourHighTicks - firstHourLowTicks;
    }

    public long getRthRangeTicks() {
        return rthHighTicks - rthLowTicks;
    }
}
