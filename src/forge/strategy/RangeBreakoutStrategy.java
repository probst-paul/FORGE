package forge.strategy;

import forge.engine.MarketContext;
import forge.execution.OrderRequest;
import forge.execution.OrderSide;

import java.util.Optional;

public class RangeBreakoutStrategy implements TradingStrategy {
    private final double rangeHigh;
    private final double rangeLow;
    private final int quantity;

    public RangeBreakoutStrategy() {
        this(0, 0, 1);
    }

    public RangeBreakoutStrategy(double rangeHigh, double rangeLow, int quantity) {
        if (rangeHigh < 0) {
            throw new IllegalArgumentException("rangeHigh cannot be negative");
        }
        if (rangeLow < 0) {
            throw new IllegalArgumentException("rangeLow cannot be negative");
        }
        if (rangeHigh > 0 && rangeLow > 0 && rangeHigh <= rangeLow) {
            throw new IllegalArgumentException("rangeHigh must be greater than rangeLow");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }

        this.rangeHigh = rangeHigh;
        this.rangeLow = rangeLow;
        this.quantity = quantity;
    }

    @Override
    public String getName() {
        return "RangeBreakout";
    }

    @Override
    public Optional<OrderRequest> evaluate(MarketContext marketContext) {
        if (rangeHigh == 0 || rangeLow == 0 || marketContext.hasOpenPosition()) {
            return Optional.empty();
        }

        if (marketContext.getLastPrice() > rangeHigh) {
            return Optional.of(OrderRequest.market(marketContext.getInstrumentSymbol(), OrderSide.BUY, quantity));
        }
        if (marketContext.getLastPrice() < rangeLow) {
            return Optional.of(OrderRequest.market(marketContext.getInstrumentSymbol(), OrderSide.SELL, quantity));
        }
        return Optional.empty();
    }

    public double getRangeHigh() {
        return rangeHigh;
    }

    public double getRangeLow() {
        return rangeLow;
    }

    public int getQuantity() {
        return quantity;
    }
}
