package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.config.MarketEventOptions;
import forge.strategy.StrategyConfigurationProfile;
import forge.event.PriceCrossoverEvent;
import forge.event.EventDirection;
import forge.event.MarketEvent;
import forge.event.FacadeForgeEvent;

import java.util.List;
import java.util.Map;

public class EventSelectionService {
    private final FacadeForgeEvent facadeEvent;

    public EventSelectionService(FacadeForgeEvent facadeEvent) {
        /*
         * Intent: Create a CLI event selection service backed by the event facade.
         * Precondition: Event facade should be available.
         * Returns: A constructed EventSelectionService instance.
         * Postcondition: Future event choices query event metadata through the supplied facade.
         */
        this.facadeEvent = facadeEvent;
    }

    /*
     * Intent: Let the user select from all available market events.
     * Precondition: At least one event must be registered in the event facade.
     * Returns: Selected MarketEvent class.
     * Postcondition: Input is consumed until a valid selection is made or the user quits.
     */
    public Class<? extends MarketEvent> selectEvent(UserInput input, UserOutput output) {
        List<Class<? extends MarketEvent>> events = facadeEvent.forgeEventAccess().findAvailableEvents();
        if (events.isEmpty()) {
            throw new IllegalStateException("No market events are available");
        }

        output.printLine("Available market events:");
        for (int i = 0; i < events.size(); i++) {
            output.printLine((i + 1) + ". " + facadeEvent.forgeEventAccess().getDisplayName(events.get(i)));
        }

        while (true) {
            int selectedIndex = input.readInt("Select market event") - 1;
            if (selectedIndex >= 0 && selectedIndex < events.size()) {
                return events.get(selectedIndex);
            }
            output.printLine("Selected market event is not available. Please select an available event, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Select a market event while honoring the selected strategy's configuration profile.
     * Precondition: Strategy profile must define a default event and any allowed event choices.
     * Returns: Default event when selection is locked, otherwise the user's selected allowed event.
     * Postcondition: User cannot select an event outside the strategy profile.
     */
    public Class<? extends MarketEvent> selectEvent(
            UserInput input,
            UserOutput output,
            StrategyConfigurationProfile strategyProfile
    ) {
        List<Class<? extends MarketEvent>> events = strategyProfile.getAllowedEvents();
        if (!strategyProfile.isEventSelectionAllowed()) {
            Class<? extends MarketEvent> event = strategyProfile.getDefaultEvent();
            output.printLine("Using market event: " + getDisplayName(event));
            return event;
        }

        output.printLine("Available market events:");
        for (int i = 0; i < events.size(); i++) {
            Class<? extends MarketEvent> event = events.get(i);
            String defaultMarker = event.equals(strategyProfile.getDefaultEvent()) ? " (default)" : "";
            output.printLine((i + 1) + ". " + getDisplayName(event) + defaultMarker);
        }

        while (true) {
            int selectedIndex = input.readInt("Select market event") - 1;
            if (selectedIndex >= 0 && selectedIndex < events.size()) {
                return events.get(selectedIndex);
            }
            output.printLine("Selected market event is not available for this strategy. Please select an available event, or enter 'quit' to exit program.");
        }
    }

    public String getDisplayName(Class<? extends MarketEvent> event) {
        return facadeEvent.forgeEventAccess().getDisplayName(event);
    }

    public boolean hasConfigurableOptions(Class<? extends MarketEvent> event) {
        return PriceCrossoverEvent.class.equals(event);
    }

    public MarketEventOptions createDefaultEventOptions(Class<? extends MarketEvent> event) {
        return facadeEvent.forgeEventAccess().createEventOptions(event);
    }

    /*
     * Intent: Read CLI configuration for a selected market event when that event has options.
     * Precondition: Event class must be supported by the event facade.
     * Returns: MarketEventOptions for the selected event.
     * Postcondition: Invalid option values are rejected and reprompted without changing data state.
     */
    public MarketEventOptions readEventOptions(
            UserInput input,
            UserOutput output,
            Class<? extends MarketEvent> event
    ) {
        if (!hasConfigurableOptions(event)) {
            return facadeEvent.forgeEventAccess().createEventOptions(event);
        }

        while (true) {
            try {
                EventDirection direction = readDirection(input, output);
                long priceThresholdTicks = input.readLong("Price threshold ticks");
                if (priceThresholdTicks <= 0) {
                    throw new IllegalArgumentException("priceThresholdTicks must be greater than zero");
                }
                return facadeEvent.forgeEventAccess().createEventOptions(
                        event,
                        Map.of(
                                "direction", direction.name(),
                                "priceThresholdTicks", Long.toString(priceThresholdTicks)
                        )
                );
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter valid event settings, or enter 'quit' to exit program.");
            }
        }
    }

    /*
     * Intent: Read the direction option for a configurable price-crossover event.
     * Precondition: User must enter one of the displayed menu choices.
     * Returns: LONG or SHORT event direction.
     * Postcondition: Invalid selection raises an exception for the caller's reprompt loop.
     */
    private EventDirection readDirection(UserInput input, UserOutput output) {
        output.printLine("Event direction:");
        output.printLine("1. Long");
        output.printLine("2. Short");
        int selectedDirection = input.readInt("Select event direction");
        if (selectedDirection == 1) {
            return EventDirection.LONG;
        }
        if (selectedDirection == 2) {
            return EventDirection.SHORT;
        }
        throw new IllegalArgumentException("Selected event direction is not available");
    }
}
