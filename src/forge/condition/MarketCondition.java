package forge.condition;

import forge.engine.MarketContext;

public interface MarketCondition {
    default String getName() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Condition")) {
            return simpleName.substring(0, simpleName.length() - "Condition".length());
        }
        return simpleName;
    }

    default ConditionResult evaluate(MarketContext marketContext) {
        return ConditionResult.notConditioned();
    }
}
