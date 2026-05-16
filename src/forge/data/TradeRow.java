package forge.data;

import java.time.Instant;
import java.util.Objects;

public class TradeRow {
    public static final int BUY_AGGRESSOR = 1;
    public static final int SELL_AGGRESSOR = -1;

    private final Instant tradeDateTime;
    private final float price;
    private final Float bidPrice;
    private final Float askPrice;
    private final long quantity;
    private final int side;
    private final long numTrades;
    private final long scidRecordIndex;

    public TradeRow(
            Instant tradeDateTime,
            float price,
            Float bidPrice,
            Float askPrice,
            long quantity,
            int side,
            long numTrades,
            long scidRecordIndex
    ) {
        this.tradeDateTime = Objects.requireNonNull(tradeDateTime, "tradeDateTime is required");
        this.price = requireFinite(price, "price must be finite");
        this.bidPrice = bidPrice == null ? null : requireFinite(bidPrice, "bidPrice must be finite");
        this.askPrice = askPrice == null ? null : requireFinite(askPrice, "askPrice must be finite");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        if (numTrades < 0) {
            throw new IllegalArgumentException("numTrades cannot be negative");
        }
        if (scidRecordIndex < 1) {
            throw new IllegalArgumentException("scidRecordIndex must be greater than zero");
        }
        if (side != BUY_AGGRESSOR && side != SELL_AGGRESSOR) {
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

    public float getPrice() {
        return price;
    }

    public Float getBidPrice() {
        return bidPrice;
    }

    public Float getAskPrice() {
        return askPrice;
    }

    public long getQuantity() {
        return quantity;
    }

    public int getSide() {
        return side;
    }

    public long getNumTrades() {
        return numTrades;
    }

    public long getScidRecordIndex() {
        return scidRecordIndex;
    }

    private static float requireFinite(float value, String message) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
