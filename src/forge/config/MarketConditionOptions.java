package forge.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MarketConditionOptions {
    private final String conditionName;
    private final Map<String, String> parameters;

    public MarketConditionOptions(String conditionName) {
        /*
         * Intent: Create condition options for conditions that do not require extra parameters.
         * Precondition: Condition name must be nonblank.
         * Returns: A constructed MarketConditionOptions instance.
         * Postcondition: Parameter map is empty and immutable.
         */
        this(conditionName, Collections.emptyMap());
    }

    public MarketConditionOptions(String conditionName, Map<String, String> parameters) {
        /*
         * Intent: Store the selected market condition name and its string parameters.
         * Precondition: Condition name must be nonblank and parameter map must not be null.
         * Returns: A constructed MarketConditionOptions instance.
         * Postcondition: Parameters are defensively copied into an immutable insertion-order map.
         */
        if (conditionName == null || conditionName.trim().isEmpty()) {
            throw new IllegalArgumentException("conditionName is required");
        }
        this.conditionName = conditionName.trim();
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(parameters, "parameters is required")));
    }

    public String getConditionName() {
        return conditionName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        /*
         * Intent: Provide a readable diagnostic summary of market condition options.
         * Precondition: MarketConditionOptions must be constructed.
         * Returns: String representation of condition options.
         * Postcondition: MarketConditionOptions state is unchanged.
         */
        return "MarketConditionOptions{" +
                "conditionName='" + conditionName + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
