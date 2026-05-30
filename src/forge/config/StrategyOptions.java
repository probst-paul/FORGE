package forge.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class StrategyOptions {
    private final String strategyName;
    private final Map<String, String> parameters;

    public StrategyOptions(String strategyName) {
        /*
         * Intent: Create strategy options for strategies that do not require extra parameters.
         * Precondition: Strategy name must be nonblank.
         * Returns: A constructed StrategyOptions instance.
         * Postcondition: Parameter map is empty and immutable.
         */
        this(strategyName, Collections.emptyMap());
    }

    public StrategyOptions(String strategyName, Map<String, String> parameters) {
        /*
         * Intent: Store the selected strategy name and its string parameters.
         * Precondition: Strategy name must be nonblank and parameter map must not be null.
         * Returns: A constructed StrategyOptions instance.
         * Postcondition: Parameters are defensively copied into an immutable insertion-order map.
         */
        if (strategyName == null || strategyName.trim().isEmpty()) {
            throw new IllegalArgumentException("strategyName is required");
        }
        this.strategyName = strategyName.trim();
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(parameters, "parameters is required")));
    }

    public String getStrategyName() {
        return strategyName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        /*
         * Intent: Provide a readable diagnostic summary of strategy options.
         * Precondition: StrategyOptions must be constructed.
         * Returns: String representation of strategy options.
         * Postcondition: StrategyOptions state is unchanged.
         */
        return "StrategyOptions{" +
                "strategyName='" + strategyName + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
