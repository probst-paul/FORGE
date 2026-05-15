package forge.trigger;

import forge.config.TradeTriggerOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class FacadeForgeTrigger {
    private static final FacadeForgeTrigger THE_INSTANCE = new FacadeForgeTrigger();

    private final TriggerCatalog triggerCatalog;

    public static FacadeForgeTrigger getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeTrigger() {
        this(new TriggerCatalog());
    }

    public FacadeForgeTrigger(TriggerCatalog triggerCatalog) {
        this.triggerCatalog = triggerCatalog;
    }

    public List<Class<? extends TradeTrigger>> findAvailableTriggers() {
        return triggerCatalog.findAvailableTriggers();
    }

    public String getDisplayName(Class<? extends TradeTrigger> trigger) {
        return triggerCatalog.getDisplayName(trigger);
    }

    public TradeTriggerOptions createTriggerOptions(Class<? extends TradeTrigger> trigger) {
        return new TradeTriggerOptions(getDisplayName(trigger));
    }

    public TradeTrigger createTrigger(Class<? extends TradeTrigger> trigger) {
        try {
            return trigger.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to create trigger " + trigger.getSimpleName(), e);
        }
    }
}
