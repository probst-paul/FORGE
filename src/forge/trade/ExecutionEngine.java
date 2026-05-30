package forge.trade;

import forge.data.market.TradeTick;

import java.util.Optional;

public interface ExecutionEngine {
    /*
     * Intent: Attempt to execute an order request against the current market tick.
     * Precondition: orderRequest and currentTick must be valid for the concrete engine.
     * Returns: Optional fill when the order is executed; Optional.empty() when it is not filled.
     * Postcondition: Concrete engine defines whether any execution state changes.
     */
    Optional<Fill> execute(OrderRequest orderRequest, TradeTick currentTick);
}
