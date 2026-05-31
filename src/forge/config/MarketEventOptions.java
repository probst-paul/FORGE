package forge.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MarketEventOptions {
    private final String eventName;
    private final Map<String, String> parameters;

    public MarketEventOptions(String eventName) {
        /*
         * Intent: Create event options for conditions that do not require extra parameters.
         * Precondition: Event name must be nonblank.
         * Returns: A constructed MarketEventOptions instance.
         * Postcondition: Parameter map is empty and immutable.
         */
        this(eventName, Collections.emptyMap());
    }

    public MarketEventOptions(String eventName, Map<String, String> parameters) {
        /*
         * Intent: Store the selected market event name and its string parameters.
         * Precondition: Event name must be nonblank and parameter map must not be null.
         * Returns: A constructed MarketEventOptions instance.
         * Postcondition: Parameters are defensively copied into an immutable insertion-order map.
         */
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("eventName is required");
        }
        this.eventName = eventName.trim();
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(parameters, "parameters is required")));
    }

    public String getEventName() {
        return eventName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        /*
         * Intent: Provide a readable diagnostic summary of market event options.
         * Precondition: MarketEventOptions must be constructed.
         * Returns: String representation of event options.
         * Postcondition: MarketEventOptions state is unchanged.
         */
        return "MarketEventOptions{" +
                "eventName='" + eventName + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
