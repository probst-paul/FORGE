package forge.event;

import forge.config.MarketEventOptions;
import forge.data.market.TradeTick;
import forge.feature.SessionRangeFeature;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FacadeForgeEvent {
    private static final FacadeForgeEvent THE_INSTANCE = new FacadeForgeEvent();

    private final EventCatalog eventCatalog;
    private final EventBuildService eventBuildService;
    private final ForgeEventAccess access = new ForgeEventAccess();

    public static FacadeForgeEvent getTheInstance() {
        /*
         * Intent: Provide the shared event facade used by application and CLI wiring.
         * Precondition: Static facade instance must have initialized successfully.
         * Returns: Singleton FacadeForgeEvent instance.
         * Postcondition: No new facade is created.
         */
        return THE_INSTANCE;
    }

    public FacadeForgeEvent() {
        /*
         * Intent: Create the event facade with default catalog and build services.
         * Precondition: Default event dependencies must be available.
         * Returns: A constructed FacadeForgeEvent instance.
         * Postcondition: Facade can expose event discovery and detection operations.
         */
        this(new EventCatalog(), new EventBuildService());
    }

    public FacadeForgeEvent(EventCatalog eventCatalog) {
        /*
         * Intent: Create the event facade with an explicit catalog and default build service.
         * Precondition: Catalog must satisfy event discovery needs.
         * Returns: A constructed FacadeForgeEvent instance.
         * Postcondition: Discovery uses the supplied catalog.
         */
        this(eventCatalog, new EventBuildService());
    }

    public FacadeForgeEvent(EventBuildService eventBuildService) {
        /*
         * Intent: Create the event facade with a default catalog and explicit build service.
         * Precondition: Build service must satisfy derived-event detection needs.
         * Returns: A constructed FacadeForgeEvent instance.
         * Postcondition: Event build operations use the supplied service.
         */
        this(new EventCatalog(), eventBuildService);
    }

    public FacadeForgeEvent(EventCatalog eventCatalog, EventBuildService eventBuildService) {
        /*
         * Intent: Create the event facade with explicit package dependencies.
         * Precondition: Catalog and build service should be non-null and usable.
         * Returns: A constructed FacadeForgeEvent instance.
         * Postcondition: Facade delegates event work to the supplied dependencies.
         */
        this.eventCatalog = eventCatalog;
        if (eventBuildService == null) {
            throw new IllegalArgumentException("eventBuildService is required");
        }
        this.eventBuildService = eventBuildService;
    }

    public ForgeEventAccess forgeEventAccess() {
        /*
         * Intent: Expose the public access object for event package operations.
         * Precondition: Facade must be constructed.
         * Returns: Stable ForgeEventAccess instance.
         * Postcondition: Facade state is unchanged.
         */
        return access;
    }

    public class ForgeEventAccess {
        public List<Class<? extends MarketEvent>> findAvailableEvents() {
            /*
             * Intent: List event implementations available for user selection or strategy profiles.
             * Precondition: Event catalog must be configured.
             * Returns: Discovered event classes.
             * Postcondition: No event instances are created.
             */
            return eventCatalog.findAvailableEvents();
        }

        public String getDisplayName(Class<? extends MarketEvent> event) {
            /*
             * Intent: Provide the user-facing name for a event class.
             * Precondition: Event class must not be null.
             * Returns: Display name from the event catalog.
             * Postcondition: Catalog and event class are unchanged.
             */
            return eventCatalog.getDisplayName(event);
        }

        public MarketEventOptions createEventOptions(Class<? extends MarketEvent> event) {
            /*
             * Intent: Create event options using default/no parameter values.
             * Precondition: Event class must be supported by display-name lookup.
             * Returns: MarketEventOptions identified by event display name.
             * Postcondition: No condition instance is created.
             */
            return new MarketEventOptions(getDisplayName(event));
        }

        public MarketEventOptions createEventOptions(Class<? extends MarketEvent> event, Map<String, String> parameters) {
            /*
             * Intent: Create event options with caller-provided parameter values.
             * Precondition: Event class and parameters must be valid for the config model.
             * Returns: MarketEventOptions identified by display name and parameter map.
             * Postcondition: Parameter map is handed to config object construction; facade state is unchanged.
             */
            return new MarketEventOptions(getDisplayName(event), parameters);
        }

        public MarketEvent createEvent(Class<? extends MarketEvent> event) {
            /*
             * Intent: Instantiate a selected event implementation through its no-argument constructor.
             * Precondition: Event class must expose an accessible no-argument constructor.
             * Returns: New MarketEvent instance.
             * Postcondition: Reflection failures are wrapped as IllegalStateException with event context.
             */
            try {
                return event.getDeclaredConstructor().newInstance();
            } catch (InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException
                     | NoSuchMethodException e) {
                throw new IllegalStateException("Unable to create event " + event.getSimpleName(), e);
            }
        }

        public List<String> getSupportedEventNames() {
            /*
             * Intent: Expose derived event names supported by the build service.
             * Precondition: Build service must be configured.
             * Returns: Supported event names.
             * Postcondition: Build service state is unchanged.
             */
            return eventBuildService.getSupportedEventNames();
        }

        public List<MarketEventOccurrence> detectFirstHourBreachEvents(
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<TradeTick> ticks
        ) {
            /*
             * Intent: Detect first-hour breach occurrences through the event package facade.
             * Precondition: Session range features and ticks must be non-null and related to the selected contracts.
             * Returns: Detected market event occurrences.
             * Postcondition: Inputs are not modified.
             */
            return eventBuildService.detectFirstHourBreachEvents(sessionRangeFeatures, ticks);
        }
    }
}
