package forge.strategy;

import forge.config.TargetSettings;
import forge.trigger.TradeTrigger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StrategyConfigurationProfile {
    private final Class<? extends TradingStrategy> strategyClass;
    private final List<Class<? extends TradeTrigger>> allowedTriggers;
    private final Class<? extends TradeTrigger> defaultTrigger;
    private final boolean triggerSelectionAllowed;
    private final List<String> allowedTargets;
    private final String defaultTarget;
    private final boolean targetSelectionAllowed;
    private final Map<String, TargetSettings> defaultTargetSettingsByTarget;

    public StrategyConfigurationProfile(
            Class<? extends TradingStrategy> strategyClass,
            List<Class<? extends TradeTrigger>> allowedTriggers,
            Class<? extends TradeTrigger> defaultTrigger,
            boolean triggerSelectionAllowed,
            List<String> allowedTargets,
            String defaultTarget,
            boolean targetSelectionAllowed,
            Map<String, TargetSettings> defaultTargetSettingsByTarget
    ) {
        this.strategyClass = Objects.requireNonNull(strategyClass, "strategyClass is required");
        this.allowedTriggers = validateChoices(allowedTriggers, "allowedTriggers");
        this.defaultTrigger = validateDefault(defaultTrigger, this.allowedTriggers, "defaultTrigger");
        this.triggerSelectionAllowed = triggerSelectionAllowed && this.allowedTriggers.size() > 1;
        this.allowedTargets = validateTargetChoices(allowedTargets, "allowedTargets");
        this.defaultTarget = validateDefaultTarget(defaultTarget, this.allowedTargets, "defaultTarget");
        this.targetSelectionAllowed = targetSelectionAllowed && this.allowedTargets.size() > 1;
        this.defaultTargetSettingsByTarget = validateDefaultTargetSettings(defaultTargetSettingsByTarget, this.allowedTargets);
    }

    public Class<? extends TradingStrategy> getStrategyClass() {
        return strategyClass;
    }

    public List<Class<? extends TradeTrigger>> getAllowedTriggers() {
        return allowedTriggers;
    }

    public Class<? extends TradeTrigger> getDefaultTrigger() {
        return defaultTrigger;
    }

    public boolean isTriggerSelectionAllowed() {
        return triggerSelectionAllowed;
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

    private <T> List<Class<? extends T>> validateChoices(List<Class<? extends T>> choices, String name) {
        Objects.requireNonNull(choices, name + " is required");
        if (choices.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one choice");
        }
        for (Class<? extends T> choice : choices) {
            Objects.requireNonNull(choice, name + " cannot contain null choices");
        }
        return Collections.unmodifiableList(List.copyOf(choices));
    }

    private <T> Class<? extends T> validateDefault(
            Class<? extends T> defaultChoice,
            List<Class<? extends T>> allowedChoices,
            String name
    ) {
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

    private List<String> validateTargetChoices(List<String> choices, String name) {
        Objects.requireNonNull(choices, name + " is required");
        if (choices.isEmpty()) {
            throw new IllegalArgumentException(name + " must contain at least one choice");
        }
        for (String choice : choices) {
            if (choice == null || choice.trim().isEmpty()) {
                throw new IllegalArgumentException(name + " cannot contain blank choices");
            }
        }
        return Collections.unmodifiableList(List.copyOf(choices));
    }

    private String validateDefaultTarget(
            String defaultChoice,
            List<String> allowedChoices,
            String name
    ) {
        if (defaultChoice == null || defaultChoice.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (!allowedChoices.contains(defaultChoice)) {
            throw new IllegalArgumentException(name + " must be included in allowed choices");
        }
        return defaultChoice;
    }
}
