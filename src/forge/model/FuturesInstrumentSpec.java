package forge.model;

public class FuturesInstrumentSpec {
    private final String symbolCode;
    private final String displayName;
    private final double tickSize;
    private final double tickDollarAmount;

    public FuturesInstrumentSpec(String symbolCode, String displayName, double tickSize, double tickDollarAmount) {
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
        this.symbolCode = symbolCode.trim().toUpperCase();
        this.displayName = displayName.trim();
        this.tickSize = tickSize;
        this.tickDollarAmount = tickDollarAmount;
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
}
