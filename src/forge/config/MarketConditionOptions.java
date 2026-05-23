package forge.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MarketConditionOptions {
    private final String conditionName;
    private final Map<String, String> parameters;

    public MarketConditionOptions(String conditionName) {
        this(conditionName, Collections.emptyMap());
    }

    public MarketConditionOptions(String conditionName, Map<String, String> parameters) {
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
        return "MarketConditionOptions{" +
                "conditionName='" + conditionName + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
