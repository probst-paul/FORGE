package forge.target;

import forge.execution.OrderSide;

public interface TargetModel {
    String getName();

    TargetResult calculateTarget(
            OrderSide side,
            long entryPriceTicks,
            long stopPriceTicks
    );
}
