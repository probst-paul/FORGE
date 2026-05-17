package forge.data.market;

import java.time.Instant;

public class TradeTick {
    private final String contractSymbol;
    private final Instant tradeDateTime;
    private final long priceTicks;
    private final Long bidPriceTicks;
    private final Long askPriceTicks;
    private final long quantity;
    private final int side;
    private final long scidRecordIndex;

    public TradeTick(
            String contractSymbol,
            Instant tradeDateTime,
            long priceTicks,
            Long bidPriceTicks,
            Long askPriceTicks,
            long quantity,
            int side,
            long scidRecordIndex
    ) {
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
        }
        if (tradeDateTime == null) {
            throw new IllegalArgumentException("tradeDateTime is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (side != 1 && side != -1) {
            throw new IllegalArgumentException("side must be 1 or -1");
        }
        if (scidRecordIndex < 1) {
            throw new IllegalArgumentException("scidRecordIndex must be positive");
        }
        this.contractSymbol = contractSymbol.trim().toUpperCase();
        this.tradeDateTime = tradeDateTime;
        this.priceTicks = priceTicks;
        this.bidPriceTicks = bidPriceTicks;
        this.askPriceTicks = askPriceTicks;
        this.quantity = quantity;
        this.side = side;
        this.scidRecordIndex = scidRecordIndex;
    }

    public String getContractSymbol() {
        return contractSymbol;
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

    public int getSide() {
        return side;
    }

    public long getScidRecordIndex() {
        return scidRecordIndex;
    }
}
