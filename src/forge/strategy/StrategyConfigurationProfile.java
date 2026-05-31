package forge.strategy;

import forge.event.MarketEvent;
import forge.util.ImmutableLists;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class StrategyConfigurationProfile {
    private final Class<? extends TradingStrategy> strategyClass;
    private final List<Class<? extends MarketEvent>> allowedEvents;
    private final Class<? extends MarketEvent> defaultEvent;
    private final boolean eventSelectionAllowed;

    public StrategyConfigurationProfile(
            Class<? extends TradingStrategy> strategyClass,
            List<Class<? extends MarketEvent>> allowedEvents,
            Class<? extends MarketEvent> defaultEvent,
            boolean eventSelectionAllowed
    ) {
        this.strategyClass = Objects.requireNonNull(strategyClass, "strategyClass is required");
        this.allowedEvents = validateChoices(
                allowedEvents,
                "allowedEvents",
                choice -> Objects.requireNonNull(choice, "allowedEvents cannot contain null choices")
        );
        this.defaultEvent = validateDefault(defaultEvent, this.allowedEvents, "defaultEvent");
        this.eventSelectionAllowed = eventSelectionAllowed && this.allowedEvents.size() > 1;
    }

    public Class<? extends TradingStrategy> getStrategyClass() {
        return strategyClass;
    }

    public List<Class<? extends MarketEvent>> getAllowedEvents() {
        return allowedEvents;
    }

    public Class<? extends MarketEvent> getDefaultEvent() {
        return defaultEvent;
    }

    public boolean isEventSelectionAllowed() {
        return eventSelectionAllowed;
    }

    private <T> List<T> validateChoices(List<? extends T> choices, String name, Function<T, T> normalizer) {
        /*
         * Intent: Validate and normalize a generic strategy configuration choice list.
         * Precondition: choices must be non-null/non-empty and normalizer must return valid choices.
         * Returns: Immutable normalized list preserving choice order.
         * Postcondition: Invalid or mutable caller-owned choice lists cannot leak into the profile.
         */
        Objects.requireNonNull(choices, name + " is required");
        if (choices.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one choice");
        }
        List<T> normalized = new java.util.ArrayList<>();
        for (T choice : choices) {
            normalized.add(normalizer.apply(choice));
        }
        return ImmutableLists.copyOfRequired(normalized, name);
    }

    private <T> Class<? extends T> validateDefault(
            Class<? extends T> defaultChoice,
            List<Class<? extends T>> allowedChoices,
            String name
    ) {
        /*
         * Intent: Validate that a generic class-based default is part of its allowed choices.
         * Precondition: defaultChoice must be non-null and allowedChoices must be normalized.
         * Returns: The accepted default class.
         * Postcondition: Profiles cannot reference unavailable defaults.
         */
        Objects.requireNonNull(defaultChoice, name + " is required");
        if (!allowedChoices.contains(defaultChoice)) {
            throw new IllegalArgumentException(name + " must be included in allowed choices");
        }
        return defaultChoice;
    }

}
