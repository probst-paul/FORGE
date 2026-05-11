package forge.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class StrategyOptions {
    private final String strategyName;
    private final Map<String, String> parameters;

    public StrategyOptions(String strategyName) {
        this(strategyName, Collections.emptyMap());
    }

    public StrategyOptions(String strategyName, Map<String, String> parameters) {
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
        return "StrategyOptions{" +
                "strategyName='" + strategyName + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
