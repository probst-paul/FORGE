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

    public boolean supportsMonthCode(String monthCode) {
        return monthCode != null && supportedMonthCodes.contains(monthCode.trim().toUpperCase());
    }

    public Set<String> getSupportedMonthCodes() {
        return supportedMonthCodes;
    }

    private Set<String> normalizeMonthCodes(Set<String> monthCodes) {
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
}
