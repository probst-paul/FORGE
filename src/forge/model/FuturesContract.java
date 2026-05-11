package forge.model;

import java.time.LocalDate;
import java.util.Objects;

public class FuturesContract extends Instrument {
    private final double tickSize;
    private final double tickDollarAmount;
    private final LocalDate expirationDate;

    public FuturesContract(
            String symbolCode,
            String displayName,
            double tickSize,
            double tickDollarAmount,
            LocalDate expirationDate
    ) {
        super(symbolCode, displayName);
        if (tickSize <= 0) {
            throw new IllegalArgumentException("tickSize must be greater than zero");
        }
        if (tickDollarAmount <= 0) {
            throw new IllegalArgumentException("tickDollarAmount must be greater than zero");
        }

        this.tickSize = tickSize;
        this.tickDollarAmount = tickDollarAmount;
        this.expirationDate = Objects.requireNonNull(expirationDate, "expirationDate is required");
    }

    public FuturesContract(String symbolCode, double tickSize, double tickDollarAmount, LocalDate expirationDate) {
        this(symbolCode, symbolCode, tickSize, tickDollarAmount, expirationDate);
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

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public boolean isExpiredOn(LocalDate date) {
        Objects.requireNonNull(date, "date is required");
        return !date.isBefore(expirationDate);
    }

    public double calculateDollarValueForTicks(double ticks) {
        return ticks * tickDollarAmount;
    }

    public double calculateTicksForPriceMove(double priceMove) {
        return priceMove / tickSize;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FuturesContract)) {
            return false;
        }

        FuturesContract that = (FuturesContract) other;
        return getSymbolCode().equals(that.getSymbolCode())
                && expirationDate.equals(that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSymbolCode(), expirationDate);
    }

    @Override
    public String toString() {
        return "FuturesContract{" +
                "symbolCode='" + getSymbolCode() + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                ", tickSize=" + tickSize +
                ", tickDollarAmount=" + tickDollarAmount +
                ", expirationDate=" + expirationDate +
                '}';
    }
}
