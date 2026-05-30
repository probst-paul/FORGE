package forge.trade;

import forge.data.market.TradeTick;

import java.util.Objects;
import java.util.Optional;

public class SimpleExecutionEngine implements ExecutionEngine {
    /*
     * Intent: Simulate a market fill at the current tick price for the MVP execution model.
     * Precondition: Order request and current tick must exist; order request must already be valid.
     * Returns: A Fill wrapped in Optional.
     * Postcondition: No engine state is mutated; fill uses the current tick time, price ticks, contract, and record index.
     */
    @Override
    public Optional<Fill> execute(OrderRequest orderRequest, TradeTick currentTick) {
        Objects.requireNonNull(orderRequest, "orderRequest is required");
        Objects.requireNonNull(currentTick, "currentTick is required");

        return Optional.of(new Fill(
                orderRequest.getInstrumentSymbol(),
                currentTick.getContractSymbol(),
                orderRequest.getSide(),
                orderRequest.getOrderType(),
                orderRequest.getQuantity(),
                currentTick.getTradeDateTime(),
                currentTick.getPriceTicks(),
                currentTick.getScidRecordIndex()
        ));
    }
}
