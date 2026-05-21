package forge.execution;

import forge.data.market.TradeTick;

import java.util.Objects;
import java.util.Optional;

public class SimpleExecutionEngine implements ExecutionEngine {
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
