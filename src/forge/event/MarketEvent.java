package forge.event;

import forge.engine.MarketContext;

public interface MarketEvent {
    default String getName() {
        /*
         * Intent: Derive a stable event name from the implementation class when not overridden.
         * Precondition: Implementing class should have a meaningful simple name.
         * Returns: Simple class name with the Event suffix removed when present.
         * Postcondition: Event instance is unchanged.
         */
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Event")) {
            return simpleName.substring(0, simpleName.length() - "Event".length());
        }
        return simpleName;
    }

    default EventResult evaluate(MarketContext marketContext) {
        /*
         * Intent: Provide a safe default for events that are definition-only or not yet executable.
         * Precondition: None.
         * Returns: A result indicating that the market event is not active.
         * Postcondition: Event instance and market context are unchanged.
         */
        return EventResult.notConditioned();
    }
}
