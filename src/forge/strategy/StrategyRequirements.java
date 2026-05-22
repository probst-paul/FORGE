package forge.strategy;

import forge.feature.TpoPeriod;
import forge.feature.TradingSession;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class StrategyRequirements {
    private static final StrategyRequirements NONE = builder().build();

    private final Set<String> requiredFeatureNames;
    private final Set<String> requiredEventNames;
    private final Set<TradingSession> evaluationSessions;
    private final Set<TpoPeriod> evaluationTpoPeriods;

    private StrategyRequirements(
            Set<String> requiredFeatureNames,
            Set<String> requiredEventNames,
            Set<TradingSession> evaluationSessions,
            Set<TpoPeriod> evaluationTpoPeriods
    ) {
        this.requiredFeatureNames = immutableCopy(requiredFeatureNames);
        this.requiredEventNames = immutableCopy(requiredEventNames);
        this.evaluationSessions = immutableCopy(evaluationSessions);
        this.evaluationTpoPeriods = immutableCopy(evaluationTpoPeriods);
    }

    public static StrategyRequirements none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> getRequiredFeatureNames() {
        return requiredFeatureNames;
    }

    public Set<String> getRequiredEventNames() {
        return requiredEventNames;
    }

    public Set<TradingSession> getEvaluationSessions() {
        return evaluationSessions;
    }

    public Set<TpoPeriod> getEvaluationTpoPeriods() {
        return evaluationTpoPeriods;
    }

    public boolean shouldEvaluate(StrategyContext context) {
        Objects.requireNonNull(context, "context is required");
        if (!evaluationSessions.isEmpty()
                && !evaluationSessions.contains(context.getTradingDayContext().getSession())) {
            return false;
        }
        return evaluationTpoPeriods.isEmpty() || evaluationTpoPeriods.contains(context.getTpoPeriod());
    }

    public boolean requiresFeature(String featureName) {
        return featureName != null && requiredFeatureNames.contains(featureName);
    }

    public boolean requiresEvent(String eventName) {
        return eventName != null && requiredEventNames.contains(eventName);
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public static class Builder {
        private final Set<String> requiredFeatureNames = new LinkedHashSet<>();
        private final Set<String> requiredEventNames = new LinkedHashSet<>();
        private final Set<TradingSession> evaluationSessions = new LinkedHashSet<>();
        private final Set<TpoPeriod> evaluationTpoPeriods = new LinkedHashSet<>();

        public Builder requireFeature(String featureName) {
            if (featureName == null || featureName.trim().isEmpty()) {
                throw new IllegalArgumentException("featureName is required");
            }
            requiredFeatureNames.add(featureName.trim());
            return this;
        }

        public Builder requireEvent(String eventName) {
            if (eventName == null || eventName.trim().isEmpty()) {
                throw new IllegalArgumentException("eventName is required");
            }
            requiredEventNames.add(eventName.trim());
            return this;
        }

        public Builder evaluateDuring(TradingSession session) {
            evaluationSessions.add(Objects.requireNonNull(session, "session is required"));
            return this;
        }

        public Builder evaluateDuring(TpoPeriod period) {
            evaluationTpoPeriods.add(Objects.requireNonNull(period, "period is required"));
            return this;
        }

        public StrategyRequirements build() {
            return new StrategyRequirements(
                    requiredFeatureNames,
                    requiredEventNames,
                    evaluationSessions,
                    evaluationTpoPeriods
            );
        }
    }
}
