package forge.strategy;

import forge.config.StrategyOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class FacadeForgeStrategy {
    private static final FacadeForgeStrategy THE_INSTANCE = new FacadeForgeStrategy();

    private final StrategyCatalog strategyCatalog;
    private final ForgeStrategyAccess access = new ForgeStrategyAccess();

    public static FacadeForgeStrategy getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeStrategy() {
        this(new StrategyCatalog());
    }

    public FacadeForgeStrategy(StrategyCatalog strategyCatalog) {
        this.strategyCatalog = strategyCatalog;
    }

    public ForgeStrategyAccess forgeStrategyAccess() {
        return access;
    }

    public class ForgeStrategyAccess {
        /*
         * Intent: Expose discovered strategy classes through the strategy facade.
         * Precondition: Strategy catalog must be initialized.
         * Returns: List of available TradingStrategy classes.
         * Postcondition: Callers do not need direct access to strategy discovery internals.
         */
        public List<Class<? extends TradingStrategy>> findAvailableStrategies() {
            return strategyCatalog.findAvailableStrategies();
        }

        /*
         * Intent: Get the display name for a strategy class through the facade.
         * Precondition: strategy must be non-null.
         * Returns: User-facing strategy name.
         * Postcondition: Strategy class is not instantiated.
         */
        public String getDisplayName(Class<? extends TradingStrategy> strategy) {
            return strategyCatalog.getDisplayName(strategy);
        }

        public String getDescription(Class<? extends TradingStrategy> strategy) {
            return strategyCatalog.getDescription(strategy);
        }

        public StrategyOptions createStrategyOptions(Class<? extends TradingStrategy> strategy) {
            return new StrategyOptions(getDisplayName(strategy));
        }

        /*
         * Intent: Retrieve CLI/GUI configuration rules for a strategy.
         * Precondition: strategy must have a catalog configuration profile.
         * Returns: StrategyConfigurationProfile.
         * Postcondition: Configuration details stay centralized in the strategy package.
         */
        public StrategyConfigurationProfile getConfigurationProfile(Class<? extends TradingStrategy> strategy) {
            return strategyCatalog.getConfigurationProfile(strategy);
        }

        /*
         * Intent: Instantiate a strategy implementation using its no-argument constructor.
         * Precondition: strategy class must expose an accessible no-argument constructor.
         * Returns: New TradingStrategy instance.
         * Postcondition: Reflection failures are wrapped in an IllegalStateException.
         */
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
}
