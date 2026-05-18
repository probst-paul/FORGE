package forge.trigger;

import forge.engine.MarketContext;

public interface TradeTrigger {
    default String getName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Trigger")) {
            return simpleName.substring(0, simpleName.length() - "Trigger".length());
        }
        return simpleName;
    }

    default TriggerResult evaluate(MarketContext marketContext) {
        return TriggerResult.notTriggered();
    }
}
