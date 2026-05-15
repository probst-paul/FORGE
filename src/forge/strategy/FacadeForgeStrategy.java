package forge.strategy;

import forge.config.StrategyOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class FacadeForgeStrategy {
    private static final FacadeForgeStrategy THE_INSTANCE = new FacadeForgeStrategy();

    private final StrategyCatalog strategyCatalog;

    public static FacadeForgeStrategy getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeStrategy() {
        this(new StrategyCatalog());
    }

    public FacadeForgeStrategy(StrategyCatalog strategyCatalog) {
        this.strategyCatalog = strategyCatalog;
    }

    public List<Class<? extends TradingStrategy>> findAvailableStrategies() {
        return strategyCatalog.findAvailableStrategies();
    }

    public String getDisplayName(Class<? extends TradingStrategy> strategy) {
        return strategyCatalog.getDisplayName(strategy);
    }

    public StrategyOptions createStrategyOptions(Class<? extends TradingStrategy> strategy) {
        return new StrategyOptions(getDisplayName(strategy));
    }

    public TradingStrategy createStrategy(Class<? extends TradingStrategy> strategy) {
        try {
            return strategy.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to create strategy " + strategy.getSimpleName(), e);
        }
    }
}
