package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.config.TradeTriggerOptions;
import forge.strategy.StrategyConfigurationProfile;
import forge.trigger.PriceCrossoverTrigger;
import forge.trigger.TriggerDirection;
import forge.trigger.TradeTrigger;
import forge.trigger.FacadeForgeTrigger;

import java.util.List;
import java.util.Map;

public class TriggerSelectionService {
    private final FacadeForgeTrigger facadeTrigger;

    public TriggerSelectionService(FacadeForgeTrigger facadeTrigger) {
        this.facadeTrigger = facadeTrigger;
    }

    public Class<? extends TradeTrigger> selectTrigger(UserInput input, UserOutput output) {
        List<Class<? extends TradeTrigger>> triggers = facadeTrigger.forgeTriggerAccess().findAvailableTriggers();
        if (triggers.isEmpty()) {
            throw new IllegalStateException("No trade triggers are available");
        }

        output.printLine("Available trade triggers:");
        for (int i = 0; i < triggers.size(); i++) {
            output.printLine((i + 1) + ". " + facadeTrigger.forgeTriggerAccess().getDisplayName(triggers.get(i)));
        }

        while (true) {
            int selectedIndex = input.readInt("Select trade trigger") - 1;
            if (selectedIndex >= 0 && selectedIndex < triggers.size()) {
                return triggers.get(selectedIndex);
            }
            output.printLine("Selected trade trigger is not available. Please select an available trigger, or enter 'quit' to exit program.");
        }
    }

    public Class<? extends TradeTrigger> selectTrigger(
            UserInput input,
            UserOutput output,
            StrategyConfigurationProfile strategyProfile
    ) {
        List<Class<? extends TradeTrigger>> triggers = strategyProfile.getAllowedTriggers();
        if (!strategyProfile.isTriggerSelectionAllowed()) {
            Class<? extends TradeTrigger> trigger = strategyProfile.getDefaultTrigger();
            output.printLine("Using trade trigger: " + getDisplayName(trigger));
            return trigger;
        }

        output.printLine("Available trade triggers:");
        for (int i = 0; i < triggers.size(); i++) {
            Class<? extends TradeTrigger> trigger = triggers.get(i);
            String defaultMarker = trigger.equals(strategyProfile.getDefaultTrigger()) ? " (default)" : "";
            output.printLine((i + 1) + ". " + getDisplayName(trigger) + defaultMarker);
        }

        while (true) {
            int selectedIndex = input.readInt("Select trade trigger") - 1;
            if (selectedIndex >= 0 && selectedIndex < triggers.size()) {
                return triggers.get(selectedIndex);
            }
            output.printLine("Selected trade trigger is not available for this strategy. Please select an available trigger, or enter 'quit' to exit program.");
        }
    }

    public String getDisplayName(Class<? extends TradeTrigger> trigger) {
        return facadeTrigger.forgeTriggerAccess().getDisplayName(trigger);
    }

    public boolean hasConfigurableOptions(Class<? extends TradeTrigger> trigger) {
        return PriceCrossoverTrigger.class.equals(trigger);
    }

    public TradeTriggerOptions readTriggerOptions(
            UserInput input,
            UserOutput output,
            Class<? extends TradeTrigger> trigger
    ) {
        if (!hasConfigurableOptions(trigger)) {
            return facadeTrigger.forgeTriggerAccess().createTriggerOptions(trigger);
        }

        while (true) {
            try {
                TriggerDirection direction = readDirection(input, output);
                long priceThresholdTicks = input.readLong("Price threshold ticks");
                if (priceThresholdTicks <= 0) {
                    throw new IllegalArgumentException("priceThresholdTicks must be greater than zero");
                }
                return facadeTrigger.forgeTriggerAccess().createTriggerOptions(
                        trigger,
                        Map.of(
                                "direction", direction.name(),
                                "priceThresholdTicks", Long.toString(priceThresholdTicks)
                        )
                );
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter valid trigger settings, or enter 'quit' to exit program.");
            }
        }
    }

    private TriggerDirection readDirection(UserInput input, UserOutput output) {
        output.printLine("Trigger direction:");
        output.printLine("1. Long");
        output.printLine("2. Short");
        int selectedDirection = input.readInt("Select trigger direction");
        if (selectedDirection == 1) {
            return TriggerDirection.LONG;
        }
        if (selectedDirection == 2) {
            return TriggerDirection.SHORT;
        }
        throw new IllegalArgumentException("Selected trigger direction is not available");
    }
}
