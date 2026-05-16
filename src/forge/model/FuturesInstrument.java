package forge.model;

public class FuturesInstrument extends Instrument {
    private final double tickSize;
    private final double tickDollarAmount;

    public FuturesInstrument(String symbolCode, String displayName, double tickSize, double tickDollarAmount) {
        super(symbolCode, displayName);
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
        if (tickDollarAmount <= 0) {
            throw new IllegalArgumentException("tickDollarAmount must be greater than zero");
        }
        this.tickSize = tickSize;
        this.tickDollarAmount = tickDollarAmount;
    }

    @Override
    public String getInstrumentType() {
        return "FUTURES";
    }

    public double getTickSize() {
        return tickSize;
    }

    public double getTickDollarAmount() {
        return tickDollarAmount;
    }
}
