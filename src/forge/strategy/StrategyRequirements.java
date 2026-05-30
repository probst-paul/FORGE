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
        /*
         * Intent: Determine whether a strategy should evaluate against the current session/TPO context.
         * Precondition: context must be non-null.
         * Returns: true when configured session and TPO filters allow evaluation.
         * Postcondition: Requirement state is unchanged.
         */
        Objects.requireNonNull(context, "context is required");
        if (!evaluationSessions.isEmpty()
                && !evaluationSessions.contains(context.getTradingDayContext().getSession())) {
            return false;
        }
        return evaluationTpoPeriods.isEmpty() || evaluationTpoPeriods.contains(context.getTpoPeriod());
    }

    public boolean requiresFeature(String featureName) {
        /*
         * Intent: Check whether this strategy declares a dependency on a derived feature.
         * Precondition: featureName may be null.
         * Returns: true when the feature name is required.
         * Postcondition: Requirement state is unchanged.
         */
        return featureName != null && requiredFeatureNames.contains(featureName);
    }

    public boolean requiresEvent(String eventName) {
        /*
         * Intent: Check whether this strategy declares a dependency on a market event.
         * Precondition: eventName may be null.
         * Returns: true when the event name is required.
         * Postcondition: Requirement state is unchanged.
         */
        return eventName != null && requiredEventNames.contains(eventName);
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        /*
         * Intent: Defensively copy a requirement set while preserving insertion order.
         * Precondition: values must be non-null.
         * Returns: Unmodifiable copy of the set.
         * Postcondition: Later builder mutations cannot affect constructed requirements.
         */
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public static class Builder {
        private final Set<String> requiredFeatureNames = new LinkedHashSet<>();
        private final Set<String> requiredEventNames = new LinkedHashSet<>();
        private final Set<TradingSession> evaluationSessions = new LinkedHashSet<>();
        private final Set<TpoPeriod> evaluationTpoPeriods = new LinkedHashSet<>();

        public Builder requireFeature(String featureName) {
            /*
             * Intent: Add a required derived feature to the strategy requirements under construction.
             * Precondition: featureName must be non-null and non-blank.
             * Returns: This builder for chaining.
             * Postcondition: The feature name is stored once in insertion order.
             */
            if (featureName == null || featureName.trim().isEmpty()) {
                throw new IllegalArgumentException("featureName is required");
            }
            requiredFeatureNames.add(featureName.trim());
            return this;
        }

        public Builder requireEvent(String eventName) {
            /*
             * Intent: Add a required market event to the strategy requirements under construction.
             * Precondition: eventName must be non-null and non-blank.
             * Returns: This builder for chaining.
             * Postcondition: The event name is stored once in insertion order.
             */
            if (eventName == null || eventName.trim().isEmpty()) {
                throw new IllegalArgumentException("eventName is required");
            }
            requiredEventNames.add(eventName.trim());
            return this;
        }

        public Builder evaluateDuring(TradingSession session) {
            /*
             * Intent: Restrict evaluation to a trading session.
             * Precondition: session must be non-null.
             * Returns: This builder for chaining.
             * Postcondition: The session is included in the allowed evaluation sessions.
             */
            evaluationSessions.add(Objects.requireNonNull(session, "session is required"));
            return this;
        }

        public Builder evaluateDuring(TpoPeriod period) {
            /*
             * Intent: Restrict evaluation to a TPO period.
             * Precondition: period must be non-null.
             * Returns: This builder for chaining.
             * Postcondition: The period is included in the allowed evaluation TPO periods.
             */
            evaluationTpoPeriods.add(Objects.requireNonNull(period, "period is required"));
            return this;
        }

        public StrategyRequirements build() {
            /*
             * Intent: Create immutable strategy requirements from builder state.
             * Precondition: Builder may contain zero or more requirements and filters.
             * Returns: StrategyRequirements instance.
             * Postcondition: Future builder mutations cannot affect the constructed requirements.
             */
            return new StrategyRequirements(
                    requiredFeatureNames,
                    requiredEventNames,
                    evaluationSessions,
                    evaluationTpoPeriods
            );
        }
    }
}
