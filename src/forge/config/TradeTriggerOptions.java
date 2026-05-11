package forge.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class TradeTriggerOptions {
    private final String triggerName;
    private final Map<String, String> parameters;

    public TradeTriggerOptions(String triggerName) {
        this(triggerName, Collections.emptyMap());
    }

    public TradeTriggerOptions(String triggerName, Map<String, String> parameters) {
        if (triggerName == null || triggerName.trim().isEmpty()) {
            throw new IllegalArgumentException("triggerName is required");
        }
        this.triggerName = triggerName.trim();
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(parameters, "parameters is required")));
    }

    public String getTriggerName() {
        return triggerName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "TradeTriggerOptions{" +
                "triggerName='" + triggerName + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
