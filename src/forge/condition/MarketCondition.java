package forge.condition;

import forge.engine.MarketContext;

public interface MarketCondition {
    default String getName() {
        /*
         * Intent: Derive a stable condition name from the implementation class when not overridden.
         * Precondition: Implementing class should have a meaningful simple name.
         * Returns: Simple class name with the Condition suffix removed when present.
         * Postcondition: Condition instance is unchanged.
         */
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Condition")) {
            return simpleName.substring(0, simpleName.length() - "Condition".length());
        }
        return simpleName;
    }

    default ConditionResult evaluate(MarketContext marketContext) {
        /*
         * Intent: Provide a safe default for conditions that are definition-only or not yet executable.
         * Precondition: None.
         * Returns: A result indicating that the market condition is not active.
         * Postcondition: Condition instance and market context are unchanged.
         */
        return ConditionResult.notConditioned();
    }
}
