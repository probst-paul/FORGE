package forge.strategy;

import forge.trade.OrderRequest;
import forge.trade.OrderSide;

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
    public StrategyDecision evaluate(StrategyContext strategyContext) {
        /*
         * Intent: Emit a directional market-order signal when price breaks outside the configured range.
         * Precondition: strategyContext must be non-null; configured range must have non-zero high/low values.
         * Returns: Buy signal above range, sell signal below range, or no action.
         * Postcondition: Strategy state is unchanged.
         */
        if (strategyContext == null) {
            throw new NullPointerException("strategyContext is required");
        }
        if (rangeHigh == 0 || rangeLow == 0 || strategyContext.hasOpenPosition()) {
            return StrategyDecision.noAction();
        }

        if (strategyContext.getMarketContext().getLastPrice() > rangeHigh) {
            return StrategyDecision.signal(OrderRequest.market(
                    strategyContext.getMarketContext().getInstrumentSymbol(),
                    OrderSide.BUY,
                    quantity
            ));
        }
        if (strategyContext.getMarketContext().getLastPrice() < rangeLow) {
            return StrategyDecision.signal(OrderRequest.market(
                    strategyContext.getMarketContext().getInstrumentSymbol(),
                    OrderSide.SELL,
                    quantity
            ));
        }
        return StrategyDecision.noAction();
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
