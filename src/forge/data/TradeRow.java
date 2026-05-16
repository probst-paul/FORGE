package forge.data;

import java.time.Instant;
import java.util.Objects;

public class TradeRow {
    public static final int BUY_AGGRESSOR = 1;
    public static final int SELL_AGGRESSOR = -1;

    private final Instant tradeDateTime;
    private final long priceTicks;
    private final Long bidPriceTicks;
    private final Long askPriceTicks;
    private final long quantity;
    private final Integer side;
    private final long numTrades;
    private final long scidRecordIndex;

    public TradeRow(
            Instant tradeDateTime,
            long priceTicks,
            Long bidPriceTicks,
            Long askPriceTicks,
            long quantity,
            Integer side,
            long numTrades,
            long scidRecordIndex
    ) {
        this.tradeDateTime = Objects.requireNonNull(tradeDateTime, "tradeDateTime is required");
        this.priceTicks = priceTicks;
        this.bidPriceTicks = bidPriceTicks;
        this.askPriceTicks = askPriceTicks;
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        if (numTrades < 0) {
            throw new IllegalArgumentException("numTrades cannot be negative");
        }
        if (scidRecordIndex < 1) {
            throw new IllegalArgumentException("scidRecordIndex must be greater than zero");
        }
        if (side != null && side != BUY_AGGRESSOR && side != SELL_AGGRESSOR) {
            throw new IllegalArgumentException("side must be buy or sell");
        }
        this.quantity = quantity;
        this.side = side;
        this.numTrades = numTrades;
        this.scidRecordIndex = scidRecordIndex;
    }

    public Instant getTradeDateTime() {
        return tradeDateTime;
    }

    public long getPriceTicks() {
        return priceTicks;
    }

    public Long getBidPriceTicks() {
        return bidPriceTicks;
    }

    public Long getAskPriceTicks() {
        return askPriceTicks;
    }

    public long getQuantity() {
        return quantity;
    }

    public Integer getSide() {
        return side;
    }

    public long getNumTrades() {
        return numTrades;
    }

    public long getScidRecordIndex() {
        return scidRecordIndex;
    }

}
