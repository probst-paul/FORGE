package forge.strategy;

import forge.config.TargetSettings;
import forge.condition.MarketCondition;
import forge.util.ImmutableLists;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class StrategyConfigurationProfile {
    private final Class<? extends TradingStrategy> strategyClass;
    private final List<Class<? extends MarketCondition>> allowedConditions;
    private final Class<? extends MarketCondition> defaultCondition;
    private final boolean conditionSelectionAllowed;
    private final List<String> allowedTargets;
    private final String defaultTarget;
    private final boolean targetSelectionAllowed;
    private final Map<String, TargetSettings> defaultTargetSettingsByTarget;

    public StrategyConfigurationProfile(
            Class<? extends TradingStrategy> strategyClass,
            List<Class<? extends MarketCondition>> allowedConditions,
            Class<? extends MarketCondition> defaultCondition,
            boolean conditionSelectionAllowed,
            List<String> allowedTargets,
            String defaultTarget,
            boolean targetSelectionAllowed,
            Map<String, TargetSettings> defaultTargetSettingsByTarget
    ) {
        this.strategyClass = Objects.requireNonNull(strategyClass, "strategyClass is required");
        this.allowedConditions = validateChoices(
                allowedConditions,
                "allowedConditions",
                choice -> Objects.requireNonNull(choice, "allowedConditions cannot contain null choices")
        );
        this.defaultCondition = validateDefault(defaultCondition, this.allowedConditions, "defaultCondition");
        this.conditionSelectionAllowed = conditionSelectionAllowed && this.allowedConditions.size() > 1;
        this.allowedTargets = validateChoices(allowedTargets, "allowedTargets", this::normalizeTargetChoice);
        this.defaultTarget = validateDefaultTarget(defaultTarget, this.allowedTargets, "defaultTarget");
        this.targetSelectionAllowed = targetSelectionAllowed && this.allowedTargets.size() > 1;
        this.defaultTargetSettingsByTarget = validateDefaultTargetSettings(defaultTargetSettingsByTarget, this.allowedTargets);
    }

    public Class<? extends TradingStrategy> getStrategyClass() {
        return strategyClass;
    }

    public List<Class<? extends MarketCondition>> getAllowedConditions() {
        return allowedConditions;
    }

    public Class<? extends MarketCondition> getDefaultCondition() {
        return defaultCondition;
    }

    public boolean isConditionSelectionAllowed() {
        return conditionSelectionAllowed;
    }

    public List<String> getAllowedTargets() {
        return allowedTargets;
    }

    public String getDefaultTarget() {
        return defaultTarget;
    }

    public boolean isTargetSelectionAllowed() {
        return targetSelectionAllowed;
    }

    public TargetSettings getDefaultTargetSettings(String targetMode) {
        /*
         * Intent: Retrieve the default target settings for a target mode allowed by this strategy.
         * Precondition: targetMode must match one configured target mode.
         * Returns: TargetSettings for that mode.
         * Postcondition: Profile state is unchanged.
         */
        TargetSettings settings = defaultTargetSettingsByTarget.get(targetMode);
        if (settings == null) {
            throw new IllegalArgumentException("No default target settings configured for " + targetMode);
        }
        return settings;
    }

    public String getTargetDisplayName(String targetMode) {
        if (!allowedTargets.contains(targetMode)) {
            throw new IllegalArgumentException("Target is not available for this strategy: " + targetMode);
        }
        return targetMode;
    }

    private <T> List<T> validateChoices(List<? extends T> choices, String name, Function<T, T> normalizer) {
        /*
         * Intent: Validate and normalize a generic strategy configuration choice list.
         * Precondition: choices must be non-null/non-empty and normalizer must return valid choices.
         * Returns: Immutable normalized list preserving choice order.
         * Postcondition: Invalid or mutable caller-owned choice lists cannot leak into the profile.
         */
        Objects.requireNonNull(choices, name + " is required");
        if (choices.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one choice");
        }
        List<T> normalized = new java.util.ArrayList<>();
        for (T choice : choices) {
            normalized.add(normalizer.apply(choice));
        }
        return ImmutableLists.copyOfRequired(normalized, name);
    }

    private <T> Class<? extends T> validateDefault(
            Class<? extends T> defaultChoice,
            List<Class<? extends T>> allowedChoices,
            String name
    ) {
        /*
         * Intent: Validate that a generic class-based default is part of its allowed choices.
         * Precondition: defaultChoice must be non-null and allowedChoices must be normalized.
         * Returns: The accepted default class.
         * Postcondition: Profiles cannot reference unavailable defaults.
         */
        Objects.requireNonNull(defaultChoice, name + " is required");
        if (!allowedChoices.contains(defaultChoice)) {
            throw new IllegalArgumentException(name + " must be included in allowed choices");
        }
        return defaultChoice;
    }

    private Map<String, TargetSettings> validateDefaultTargetSettings(
            Map<String, TargetSettings> settingsByTarget,
            List<String> allowedTargets
    ) {
        /*
         * Intent: Ensure each allowed target mode has matching default settings.
         * Precondition: settingsByTarget must be non-null and allowedTargets must be normalized.
         * Returns: Unmodifiable target-mode-to-settings map.
         * Postcondition: Missing default target settings are rejected at profile construction time.
         */
        Objects.requireNonNull(settingsByTarget, "defaultTargetSettingsByTarget is required");
        Map<String, TargetSettings> normalized = new LinkedHashMap<>();
        for (String targetMode : allowedTargets) {
            TargetSettings settings = settingsByTarget.get(targetMode);
            if (settings == null) {
                throw new IllegalArgumentException("Default target settings are required for " + targetMode);
            }
            normalized.put(targetMode, settings);
        }
        return Collections.unmodifiableMap(normalized);
    }

    private String validateDefaultTarget(
            String defaultChoice,
            List<String> allowedChoices,
            String name
    ) {
        String normalizedDefault = normalizeTargetChoice(defaultChoice);
        if (!allowedChoices.contains(normalizedDefault)) {
            throw new IllegalArgumentException(name + " must be included in allowed choices");
        }
        return normalizedDefault;
    }

    private String normalizeTargetChoice(String choice) {
        if (choice == null || choice.trim().isEmpty()) {
            throw new IllegalArgumentException("target choice cannot be blank");
        }
        return choice.trim();
    }
}
