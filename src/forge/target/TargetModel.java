package forge.target;

import forge.execution.OrderSide;

public interface TargetModel {
    String getName();

    TargetResult calculateTarget(
            OrderSide side,
            double entryPrice,
            double stopPrice,
            double tickSize
    );
}
