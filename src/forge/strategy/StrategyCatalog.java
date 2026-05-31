package forge.strategy;

import forge.config.TargetSettings;
import forge.event.OrderFlowExhaustionEvent;
import forge.event.PriceCrossoverEvent;
import forge.event.MarketEvent;
import forge.util.ClasspathCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StrategyCatalog {
    private static final String STRATEGY_PACKAGE = "forge.strategy";
    private final ClasspathCatalog<TradingStrategy> strategyClasspathCatalog;

    public StrategyCatalog() {
        this(new ClasspathCatalog<>(STRATEGY_PACKAGE, TradingStrategy.class));
    }

    public StrategyCatalog(ClasspathCatalog<TradingStrategy> strategyClasspathCatalog) {
        if (strategyClasspathCatalog == null) {
            throw new IllegalArgumentException("strategyClasspathCatalog is required");
        }
        this.strategyClasspathCatalog = strategyClasspathCatalog;
    }

    public List<Class<? extends TradingStrategy>> findAvailableStrategies() {
        /*
         * Intent: Discover available trading strategy implementations.
         * Precondition: Strategy classes must be visible on the classpath.
         * Returns: List of concrete TradingStrategy implementation classes.
         * Postcondition: Catalog state is unchanged.
         */
        return strategyClasspathCatalog.findImplementations();
    }

    public String getDisplayName(Class<? extends TradingStrategy> strategyClass) {
        /*
         * Intent: Convert a strategy class name into a user-facing display name.
         * Precondition: strategyClass must be non-null.
         * Returns: Simple strategy name with the Strategy suffix removed when present.
         * Postcondition: Strategy class is not instantiated.
         */
        String simpleName = strategyClass.getSimpleName();
        if (simpleName.endsWith("Strategy")) {
            return simpleName.substring(0, simpleName.length() - "Strategy".length());
        }
        return simpleName;
    }

    public String getDescription(Class<? extends TradingStrategy> strategyClass) {
        /*
         * Intent: Provide short CLI/GUI help text for a strategy.
         * Precondition: strategyClass must be non-null.
         * Returns: Strategy-specific description or a generic fallback.
         * Postcondition: Strategy class is not instantiated.
         */
        if (OpeningRangeContinuationStrategy.class.equals(strategyClass)) {
            return "Looks for the first RTH breach of the first-hour range after that range stays inside the overnight range.";
        }
        if (RangeBreakoutStrategy.class.equals(strategyClass)) {
            return "Looks for price to break beyond a configured high/low range and emits a directional order signal.";
        }
        return "Custom trading strategy.";
    }

    public StrategyConfigurationProfile getConfigurationProfile(Class<? extends TradingStrategy> strategyClass) {
        /*
         * Intent: Define which conditions and target modes a strategy allows.
         * Precondition: strategyClass must be one of the configured strategy classes.
         * Returns: StrategyConfigurationProfile for CLI/GUI setup.
         * Postcondition: Unsupported strategies are rejected before configuration is shown.
         */
        if (OpeningRangeContinuationStrategy.class.equals(strategyClass)) {
            List<Class<? extends MarketEvent>> allowedEvents = List.of(PriceCrossoverEvent.class);
            List<String> allowedTargets = List.of(TargetSettings.FIXED_TARGET);
            Map<String, TargetSettings> defaultTargetSettings = new LinkedHashMap<>();
            defaultTargetSettings.put(TargetSettings.FIXED_TARGET, TargetSettings.fixedTarget(1));
            return new StrategyConfigurationProfile(
                    strategyClass,
                    allowedEvents,
                    PriceCrossoverEvent.class,
                    false,
                    allowedTargets,
                    TargetSettings.FIXED_TARGET,
                    false,
                    defaultTargetSettings
            );
        }
        if (RangeBreakoutStrategy.class.equals(strategyClass)) {
            List<Class<? extends MarketEvent>> allowedEvents = List.of(OrderFlowExhaustionEvent.class, PriceCrossoverEvent.class);
            List<String> allowedTargets = List.of(TargetSettings.FIXED_RISK_REWARD, TargetSettings.FIXED_TARGET);
            Map<String, TargetSettings> defaultTargetSettings = new LinkedHashMap<>();
            defaultTargetSettings.put(TargetSettings.FIXED_RISK_REWARD, TargetSettings.fixedRiskReward(2.0));
            defaultTargetSettings.put(TargetSettings.FIXED_TARGET, TargetSettings.fixedTarget(8));
            return new StrategyConfigurationProfile(
                    strategyClass,
                    allowedEvents,
                    OrderFlowExhaustionEvent.class,
                    true,
                    allowedTargets,
                    TargetSettings.FIXED_RISK_REWARD,
                    true,
                    defaultTargetSettings
            );
        }
        throw new IllegalArgumentException("No configuration profile is defined for " + strategyClass.getSimpleName());
    }

}
