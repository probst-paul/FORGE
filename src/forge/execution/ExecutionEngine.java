package forge.execution;

import forge.data.market.TradeTick;

import java.util.Optional;

public interface ExecutionEngine {
    Optional<Fill> execute(OrderRequest orderRequest, TradeTick currentTick);
}
