package forge.condition;

import forge.config.MarketConditionOptions;
import forge.data.market.TradeTick;
import forge.feature.SessionRangeFeature;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FacadeForgeCondition {
    private static final FacadeForgeCondition THE_INSTANCE = new FacadeForgeCondition();

    private final ConditionCatalog conditionCatalog;
    private final ConditionBuildService conditionBuildService;
    private final ForgeConditionAccess access = new ForgeConditionAccess();

    public static FacadeForgeCondition getTheInstance() {
        /*
         * Intent: Provide the shared condition facade used by application and CLI wiring.
         * Precondition: Static facade instance must have initialized successfully.
         * Returns: Singleton FacadeForgeCondition instance.
         * Postcondition: No new facade is created.
         */
        return THE_INSTANCE;
    }

    public FacadeForgeCondition() {
        /*
         * Intent: Create the condition facade with default catalog and build services.
         * Precondition: Default condition dependencies must be available.
         * Returns: A constructed FacadeForgeCondition instance.
         * Postcondition: Facade can expose condition discovery and detection operations.
         */
        this(new ConditionCatalog(), new ConditionBuildService());
    }

    public FacadeForgeCondition(ConditionCatalog conditionCatalog) {
        /*
         * Intent: Create the condition facade with an explicit catalog and default build service.
         * Precondition: Catalog must satisfy condition discovery needs.
         * Returns: A constructed FacadeForgeCondition instance.
         * Postcondition: Discovery uses the supplied catalog.
         */
        this(conditionCatalog, new ConditionBuildService());
    }

    public FacadeForgeCondition(ConditionBuildService conditionBuildService) {
        /*
         * Intent: Create the condition facade with a default catalog and explicit build service.
         * Precondition: Build service must satisfy derived-condition detection needs.
         * Returns: A constructed FacadeForgeCondition instance.
         * Postcondition: Condition build operations use the supplied service.
         */
        this(new ConditionCatalog(), conditionBuildService);
    }

    public FacadeForgeCondition(ConditionCatalog conditionCatalog, ConditionBuildService conditionBuildService) {
        /*
         * Intent: Create the condition facade with explicit package dependencies.
         * Precondition: Catalog and build service should be non-null and usable.
         * Returns: A constructed FacadeForgeCondition instance.
         * Postcondition: Facade delegates condition work to the supplied dependencies.
         */
        this.conditionCatalog = conditionCatalog;
        if (conditionBuildService == null) {
            throw new IllegalArgumentException("conditionBuildService is required");
        }
        this.conditionBuildService = conditionBuildService;
    }

    public ForgeConditionAccess forgeConditionAccess() {
        /*
         * Intent: Expose the public access object for condition package operations.
         * Precondition: Facade must be constructed.
         * Returns: Stable ForgeConditionAccess instance.
         * Postcondition: Facade state is unchanged.
         */
        return access;
    }

    public class ForgeConditionAccess {
        public List<Class<? extends MarketCondition>> findAvailableConditions() {
            /*
             * Intent: List condition implementations available for user selection or strategy profiles.
             * Precondition: Condition catalog must be configured.
             * Returns: Discovered condition classes.
             * Postcondition: No condition instances are created.
             */
            return conditionCatalog.findAvailableConditions();
        }

        public String getDisplayName(Class<? extends MarketCondition> condition) {
            /*
             * Intent: Provide the user-facing name for a condition class.
             * Precondition: Condition class must not be null.
             * Returns: Display name from the condition catalog.
             * Postcondition: Catalog and condition class are unchanged.
             */
            return conditionCatalog.getDisplayName(condition);
        }

        public MarketConditionOptions createConditionOptions(Class<? extends MarketCondition> condition) {
            /*
             * Intent: Create condition options using default/no parameter values.
             * Precondition: Condition class must be supported by display-name lookup.
             * Returns: MarketConditionOptions identified by condition display name.
             * Postcondition: No condition instance is created.
             */
            return new MarketConditionOptions(getDisplayName(condition));
        }

        public MarketConditionOptions createConditionOptions(Class<? extends MarketCondition> condition, Map<String, String> parameters) {
            /*
             * Intent: Create condition options with caller-provided parameter values.
             * Precondition: Condition class and parameters must be valid for the config model.
             * Returns: MarketConditionOptions identified by display name and parameter map.
             * Postcondition: Parameter map is handed to config object construction; facade state is unchanged.
             */
            return new MarketConditionOptions(getDisplayName(condition), parameters);
        }

        public MarketCondition createCondition(Class<? extends MarketCondition> condition) {
            /*
             * Intent: Instantiate a selected condition implementation through its no-argument constructor.
             * Precondition: Condition class must expose an accessible no-argument constructor.
             * Returns: New MarketCondition instance.
             * Postcondition: Reflection failures are wrapped as IllegalStateException with condition context.
             */
            try {
                return condition.getDeclaredConstructor().newInstance();
            } catch (InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException
                     | NoSuchMethodException e) {
                throw new IllegalStateException("Unable to create condition " + condition.getSimpleName(), e);
            }
        }

        public List<String> getSupportedConditionNames() {
            /*
             * Intent: Expose derived condition names supported by the build service.
             * Precondition: Build service must be configured.
             * Returns: Supported condition/event names.
             * Postcondition: Build service state is unchanged.
             */
            return conditionBuildService.getSupportedConditionNames();
        }

        public List<MarketConditionOccurrence> detectFirstHourBreachConditions(
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<TradeTick> ticks
        ) {
            /*
             * Intent: Detect first-hour breach occurrences through the condition package facade.
             * Precondition: Session range features and ticks must be non-null and related to the selected contracts.
             * Returns: Detected market condition occurrences.
             * Postcondition: Inputs are not modified.
             */
            return conditionBuildService.detectFirstHourBreachConditions(sessionRangeFeatures, ticks);
        }
    }
}
