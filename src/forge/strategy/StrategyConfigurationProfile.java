package forge.strategy;

import forge.config.TargetSettings;
import forge.target.TargetModel;
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
    private final List<Class<? extends TargetModel>> allowedTargets;
    private final Class<? extends TargetModel> defaultTarget;
    private final boolean targetSelectionAllowed;
    private final Map<Class<? extends TargetModel>, TargetSettings> defaultTargetSettingsByTarget;

    public StrategyConfigurationProfile(
            Class<? extends TradingStrategy> strategyClass,
            List<Class<? extends TradeTrigger>> allowedTriggers,
            Class<? extends TradeTrigger> defaultTrigger,
            boolean triggerSelectionAllowed,
            List<Class<? extends TargetModel>> allowedTargets,
            Class<? extends TargetModel> defaultTarget,
            boolean targetSelectionAllowed,
            Map<Class<? extends TargetModel>, TargetSettings> defaultTargetSettingsByTarget
    ) {
        this.strategyClass = Objects.requireNonNull(strategyClass, "strategyClass is required");
        this.allowedTriggers = validateChoices(allowedTriggers, "allowedTriggers");
        this.defaultTrigger = validateDefault(defaultTrigger, this.allowedTriggers, "defaultTrigger");
        this.triggerSelectionAllowed = triggerSelectionAllowed && this.allowedTriggers.size() > 1;
        this.allowedTargets = validateChoices(allowedTargets, "allowedTargets");
        this.defaultTarget = validateDefault(defaultTarget, this.allowedTargets, "defaultTarget");
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

    public List<Class<? extends TargetModel>> getAllowedTargets() {
        return allowedTargets;
    }

    public Class<? extends TargetModel> getDefaultTarget() {
        return defaultTarget;
    }

    public boolean isTargetSelectionAllowed() {
        return targetSelectionAllowed;
    }

    public TargetSettings getDefaultTargetSettings(Class<? extends TargetModel> targetModel) {
        TargetSettings settings = defaultTargetSettingsByTarget.get(targetModel);
        if (settings == null) {
            throw new IllegalArgumentException("No default target settings configured for " + targetModel.getSimpleName());
        }
        return settings;
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

    private Map<Class<? extends TargetModel>, TargetSettings> validateDefaultTargetSettings(
            Map<Class<? extends TargetModel>, TargetSettings> settingsByTarget,
            List<Class<? extends TargetModel>> allowedTargets
    ) {
        Objects.requireNonNull(settingsByTarget, "defaultTargetSettingsByTarget is required");
        Map<Class<? extends TargetModel>, TargetSettings> normalized = new LinkedHashMap<>();
        for (Class<? extends TargetModel> targetModel : allowedTargets) {
            TargetSettings settings = settingsByTarget.get(targetModel);
            if (settings == null) {
                throw new IllegalArgumentException("Default target settings are required for " + targetModel.getSimpleName());
            }
            normalized.put(targetModel, settings);
        }
        return Collections.unmodifiableMap(normalized);
    }
}
