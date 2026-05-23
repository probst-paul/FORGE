package forge.strategy;

import forge.config.TargetSettings;
import forge.condition.OrderFlowExhaustionCondition;
import forge.condition.PriceCrossoverCondition;
import forge.condition.MarketCondition;
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
        return strategyClasspathCatalog.findImplementations();
    }

    public String getDisplayName(Class<? extends TradingStrategy> strategyClass) {
        String simpleName = strategyClass.getSimpleName();
        if (simpleName.endsWith("Strategy")) {
            return simpleName.substring(0, simpleName.length() - "Strategy".length());
        }
        return simpleName;
    }

    public String getDescription(Class<? extends TradingStrategy> strategyClass) {
        if (OpeningRangeContinuationStrategy.class.equals(strategyClass)) {
            return "Looks for the first RTH breach of the first-hour range after that range stays inside the overnight range.";
        }
        if (RangeBreakoutStrategy.class.equals(strategyClass)) {
            return "Looks for price to break beyond a configured high/low range and emits a directional order signal.";
        }
        return "Custom trading strategy.";
    }

    public StrategyConfigurationProfile getConfigurationProfile(Class<? extends TradingStrategy> strategyClass) {
        if (OpeningRangeContinuationStrategy.class.equals(strategyClass)) {
            List<Class<? extends MarketCondition>> allowedConditions = List.of(PriceCrossoverCondition.class);
            List<String> allowedTargets = List.of(TargetSettings.FIXED_TARGET);
            Map<String, TargetSettings> defaultTargetSettings = new LinkedHashMap<>();
            defaultTargetSettings.put(TargetSettings.FIXED_TARGET, TargetSettings.fixedTarget(1));
            return new StrategyConfigurationProfile(
                    strategyClass,
                    allowedConditions,
                    PriceCrossoverCondition.class,
                    false,
                    allowedTargets,
                    TargetSettings.FIXED_TARGET,
                    false,
                    defaultTargetSettings
            );
        }
        if (RangeBreakoutStrategy.class.equals(strategyClass)) {
            List<Class<? extends MarketCondition>> allowedConditions = List.of(OrderFlowExhaustionCondition.class, PriceCrossoverCondition.class);
            List<String> allowedTargets = List.of(TargetSettings.FIXED_RISK_REWARD, TargetSettings.FIXED_TARGET);
            Map<String, TargetSettings> defaultTargetSettings = new LinkedHashMap<>();
            defaultTargetSettings.put(TargetSettings.FIXED_RISK_REWARD, TargetSettings.fixedRiskReward(2.0));
            defaultTargetSettings.put(TargetSettings.FIXED_TARGET, TargetSettings.fixedTarget(8));
            return new StrategyConfigurationProfile(
                    strategyClass,
                    allowedConditions,
                    OrderFlowExhaustionCondition.class,
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
