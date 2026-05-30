package forge.model;

import java.util.Objects;

public abstract class Instrument {
    private final String symbolCode;
    private final String displayName;

    protected Instrument(String symbolCode, String displayName) {
        if (symbolCode == null || symbolCode.trim().isEmpty()) {
            throw new IllegalArgumentException("symbolCode is required");
        }

        this.symbolCode = symbolCode.trim().toUpperCase();
        this.displayName = normalizeDisplayName(displayName, this.symbolCode);
    }

    public String getSymbolCode() {
        return symbolCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public abstract String getInstrumentType();

    @Override
    public String toString() {
        return "Instrument{" +
                "symbolCode='" + symbolCode + '\'' +
                ", displayName='" + displayName + '\'' +
                ", instrumentType='" + getInstrumentType() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Instrument that = (Instrument) other;
        return symbolCode.equals(that.symbolCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), symbolCode);
    }

    private static String normalizeDisplayName(String displayName, String symbolCode) {
        /*
         * Intent: Ensure every instrument has a usable display name.
         * Precondition: symbolCode must already be normalized and non-blank.
         * Returns: Trimmed display name or the symbol code fallback.
         * Postcondition: Instrument construction never stores a blank display name.
         */
        if (displayName == null || displayName.trim().isEmpty()) {
            return symbolCode;
        }
        return displayName.trim();
    }
}
