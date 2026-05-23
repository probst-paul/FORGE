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
        return THE_INSTANCE;
    }

    public FacadeForgeCondition() {
        this(new ConditionCatalog(), new ConditionBuildService());
    }

    public FacadeForgeCondition(ConditionCatalog conditionCatalog) {
        this(conditionCatalog, new ConditionBuildService());
    }

    public FacadeForgeCondition(ConditionBuildService conditionBuildService) {
        this(new ConditionCatalog(), conditionBuildService);
    }

    public FacadeForgeCondition(ConditionCatalog conditionCatalog, ConditionBuildService conditionBuildService) {
        this.conditionCatalog = conditionCatalog;
        if (conditionBuildService == null) {
            throw new IllegalArgumentException("conditionBuildService is required");
        }
        this.conditionBuildService = conditionBuildService;
    }

    public ForgeConditionAccess forgeConditionAccess() {
        return access;
    }

    public class ForgeConditionAccess {
        public List<Class<? extends MarketCondition>> findAvailableConditions() {
            return conditionCatalog.findAvailableConditions();
        }

        public String getDisplayName(Class<? extends MarketCondition> condition) {
            return conditionCatalog.getDisplayName(condition);
        }

        public MarketConditionOptions createConditionOptions(Class<? extends MarketCondition> condition) {
            return new MarketConditionOptions(getDisplayName(condition));
        }

        public MarketConditionOptions createConditionOptions(Class<? extends MarketCondition> condition, Map<String, String> parameters) {
            return new MarketConditionOptions(getDisplayName(condition), parameters);
        }

        public MarketCondition createCondition(Class<? extends MarketCondition> condition) {
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
            return conditionBuildService.getSupportedConditionNames();
        }

        public List<MarketConditionOccurrence> detectFirstHourBreachConditions(
                Collection<SessionRangeFeature> sessionRangeFeatures,
                Collection<TradeTick> ticks
        ) {
            return conditionBuildService.detectFirstHourBreachConditions(sessionRangeFeatures, ticks);
        }
    }
}
