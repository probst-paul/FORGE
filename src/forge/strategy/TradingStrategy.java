package forge.strategy;

import forge.engine.MarketContext;
import forge.execution.OrderRequest;

import java.util.Optional;

public interface TradingStrategy {
    String getName();

    Optional<OrderRequest> evaluate(MarketContext marketContext);

    default void onBacktestStart() {
        // Optional lifecycle hook for strategies that maintain state.
    }
}
