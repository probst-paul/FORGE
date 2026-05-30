package forge.model;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collections;

public class FuturesInstrumentSpec {
    public static final Set<String> ALL_MONTH_CODES = Set.of("F", "G", "H", "J", "K", "M", "N", "Q", "U", "V", "X", "Z");

    private final String symbolCode;
    private final String displayName;
    private final double tickSize;
    private final double tickDollarAmount;
    private final Set<String> supportedMonthCodes;

    public FuturesInstrumentSpec(String symbolCode, String displayName, double tickSize, double tickDollarAmount) {
        this(symbolCode, displayName, tickSize, tickDollarAmount, ALL_MONTH_CODES);
    }

    public FuturesInstrumentSpec(
            String symbolCode,
            String displayName,
            double tickSize,
            double tickDollarAmount,
            Set<String> supportedMonthCodes
    ) {
        if (symbolCode == null || symbolCode.trim().isEmpty()) {
            throw new IllegalArgumentException("symbolCode is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("displayName is required");
        }
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
        if (tickDollarAmount <= 0) {
            throw new IllegalArgumentException("tickDollarAmount must be greater than zero");
        }
        if (supportedMonthCodes == null || supportedMonthCodes.isEmpty()) {
            throw new IllegalArgumentException("supportedMonthCodes is required");
        }
        this.symbolCode = symbolCode.trim().toUpperCase();
        this.displayName = displayName.trim();
        this.tickSize = tickSize;
        this.tickDollarAmount = tickDollarAmount;
        this.supportedMonthCodes = normalizeMonthCodes(supportedMonthCodes);
    }

    public String getSymbolCode() {
        return symbolCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getTickSize() {
        return tickSize;
    }

    public double getTickDollarAmount() {
        return tickDollarAmount;
    }

    public double displayPrice(long priceTicks) {
        /*
         * Intent: Convert stored integer price units into a display price.
         * Precondition: priceTicks must be positive and represent normalized minimum contract ticks.
         * Returns: Human-readable price for this instrument.
         * Postcondition: Instrument specification state is unchanged.
         */
        if (priceTicks <= 0) {
            throw new IllegalArgumentException("priceTicks must be greater than zero");
        }
        return (priceTicks / storedTickScale(priceTicks)) * tickSize;
    }

    public long contractTicksBetween(long entryPriceTicks, long exitPriceTicks) {
        /*
         * Intent: Convert two stored integer prices into an instrument tick movement.
         * Precondition: Entry and exit prices must both be normalized minimum contract ticks.
         * Returns: Signed movement in minimum contract ticks.
         * Postcondition: Instrument specification state is unchanged.
         */
        double scale = Math.max(storedTickScale(entryPriceTicks), storedTickScale(exitPriceTicks));
        return Math.round((exitPriceTicks - entryPriceTicks) / scale);
    }

    public boolean supportsMonthCode(String monthCode) {
        /*
         * Intent: Check whether this instrument supports a futures expiration month code.
         * Precondition: monthCode may be null or unnormalized.
         * Returns: true when the normalized month code is supported.
         * Postcondition: Instrument specification state is unchanged.
         */
        return monthCode != null && supportedMonthCodes.contains(monthCode.trim().toUpperCase());
    }

    public Set<String> getSupportedMonthCodes() {
        return supportedMonthCodes;
    }

    public static double displayPrice(long priceTicks, double tickSize) {
        /*
         * Intent: Convert stored integer price units into a display price when only tick size is known.
         * Precondition: priceTicks and tickSize must be positive.
         * Returns: Human-readable price using normalized minimum contract ticks.
         * Postcondition: No state is changed.
         */
        if (priceTicks <= 0) {
            throw new IllegalArgumentException("priceTicks must be greater than zero");
        }
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
        return (priceTicks / storedTickScale(priceTicks, tickSize)) * tickSize;
    }

    public static long contractTicksBetween(long entryPriceTicks, long exitPriceTicks, double tickSize) {
        /*
         * Intent: Convert two stored integer prices into contract tick movement when only tick size is known.
         * Precondition: Entry/exit prices must be normalized minimum contract ticks and tickSize must be positive.
         * Returns: Signed movement in minimum contract ticks.
         * Postcondition: No state is changed.
         */
        double scale = Math.max(
                storedTickScale(entryPriceTicks, tickSize),
                storedTickScale(exitPriceTicks, tickSize)
        );
        return Math.round((exitPriceTicks - entryPriceTicks) / scale);
    }

    private Set<String> normalizeMonthCodes(Set<String> monthCodes) {
        /*
         * Intent: Validate and normalize supported futures month codes.
         * Precondition: monthCodes must be non-null and contain non-blank month symbols.
         * Returns: Unmodifiable set of uppercase futures month codes.
         * Postcondition: Invalid month codes are rejected before the spec is constructed.
         */
        Set<String> normalizedMonthCodes = new LinkedHashSet<>();
        for (String monthCode : monthCodes) {
            if (monthCode == null || monthCode.trim().isEmpty()) {
                throw new IllegalArgumentException("supported month codes cannot be blank");
            }
            String normalizedMonthCode = monthCode.trim().toUpperCase();
            if (!ALL_MONTH_CODES.contains(normalizedMonthCode)) {
                throw new IllegalArgumentException("Unsupported futures month code: " + normalizedMonthCode);
            }
            normalizedMonthCodes.add(normalizedMonthCode);
        }
        return Collections.unmodifiableSet(normalizedMonthCodes);
    }

    private double storedTickScale(long priceTicks) {
        return storedTickScale(priceTicks, tickSize);
    }

    private static double storedTickScale(long priceTicks, double tickSize) {
        /*
         * Intent: Detect imported rows whose tick counts were scaled by source price precision.
         * Precondition: priceTicks and tickSize must describe an imported futures price.
         * Returns: 100.0 for cent-scaled tick counts; otherwise 1.0.
         * Postcondition: No state is changed.
         */
        if (tickSize < 1.0 && Math.abs(priceTicks) >= 200_000 && priceTicks % 100 == 0) {
            return 100.0;
        }
        return 1.0;
    }

}
