package forge.app;

import forge.trigger.TradeTrigger;
import forge.trigger.TriggerCatalog;

import java.util.List;

public class TriggerSelectionService {
    private final TriggerCatalog triggerCatalog;

    public TriggerSelectionService(TriggerCatalog triggerCatalog) {
        this.triggerCatalog = triggerCatalog;
    }

    public Class<? extends TradeTrigger> selectTrigger(UserInput input, UserOutput output) {
        List<Class<? extends TradeTrigger>> triggers = triggerCatalog.findAvailableTriggers();
        if (triggers.isEmpty()) {
            throw new IllegalStateException("No trade triggers are available");
        }

        output.printLine("Available trade triggers:");
        for (int i = 0; i < triggers.size(); i++) {
            output.printLine((i + 1) + ". " + triggerCatalog.getDisplayName(triggers.get(i)));
        }

        int selectedIndex = input.readInt("Select trade trigger") - 1;
        if (selectedIndex < 0 || selectedIndex >= triggers.size()) {
            throw new IllegalArgumentException("Selected trade trigger is not available");
        }
        return triggers.get(selectedIndex);
    }

    public String getDisplayName(Class<? extends TradeTrigger> trigger) {
        return triggerCatalog.getDisplayName(trigger);
    }
}
