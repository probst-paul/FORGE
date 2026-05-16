package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.trigger.TradeTrigger;
import forge.trigger.FacadeForgeTrigger;

import java.util.List;

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

    public String getDisplayName(Class<? extends TradeTrigger> trigger) {
        return facadeTrigger.forgeTriggerAccess().getDisplayName(trigger);
    }
}
