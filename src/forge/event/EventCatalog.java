package forge.event;

import forge.util.ClasspathCatalog;

import java.util.List;

public class EventCatalog {
    private static final String EVENT_PACKAGE = "forge.event";
    private final ClasspathCatalog<MarketEvent> eventClasspathCatalog;

    public EventCatalog() {
        /*
         * Intent: Create a catalog that discovers event implementations from the event package.
         * Precondition: Event classes must be on the application classpath.
         * Returns: A constructed EventCatalog instance.
         * Postcondition: Catalog can discover MarketEvent implementations.
         */
        this(new ClasspathCatalog<>(EVENT_PACKAGE, MarketEvent.class));
    }

    public EventCatalog(ClasspathCatalog<MarketEvent> eventClasspathCatalog) {
        /*
         * Intent: Create a catalog with an explicit classpath discovery helper.
         * Precondition: Discovery helper must not be null.
         * Returns: A constructed EventCatalog instance.
         * Postcondition: Future event discovery delegates to the supplied helper.
         */
        if (eventClasspathCatalog == null) {
            throw new IllegalArgumentException("eventClasspathCatalog is required");
        }
        this.eventClasspathCatalog = eventClasspathCatalog;
    }

    public List<Class<? extends MarketEvent>> findAvailableEvents() {
        /*
         * Intent: Discover available market event implementations.
         * Precondition: Classpath discovery helper must be configured for MarketEvent.
         * Returns: List of discovered event classes.
         * Postcondition: Catalog state is unchanged.
         */
        return eventClasspathCatalog.findImplementations();
    }

    public String getDisplayName(Class<? extends MarketEvent> eventClass) {
        /*
         * Intent: Convert an event class name into a user-facing display name.
         * Precondition: Event class must not be null.
         * Returns: Simple class name with the Event suffix removed when present.
         * Postcondition: Class metadata and catalog state are unchanged.
         */
        String simpleName = eventClass.getSimpleName();
        if (simpleName.endsWith("Event")) {
            return simpleName.substring(0, simpleName.length() - "Event".length());
        }
        return simpleName;
    }

}
