package forge.stop;

import forge.engine.MarketContext;
import forge.execution.OrderSide;

public interface StopModel {
    default String getName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Stop")) {
            return simpleName.substring(0, simpleName.length() - "Stop".length());
        }
        return simpleName;
    }

    StopResult evaluateStop(OrderSide positionSide, MarketContext marketContext);
}
