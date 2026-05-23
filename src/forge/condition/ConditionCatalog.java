package forge.condition;

import forge.util.ClasspathCatalog;

import java.util.List;

public class ConditionCatalog {
    private static final String CONDITION_PACKAGE = "forge.condition";
    private final ClasspathCatalog<MarketCondition> conditionClasspathCatalog;

    public ConditionCatalog() {
        this(new ClasspathCatalog<>(CONDITION_PACKAGE, MarketCondition.class));
    }

    public ConditionCatalog(ClasspathCatalog<MarketCondition> conditionClasspathCatalog) {
        if (conditionClasspathCatalog == null) {
            throw new IllegalArgumentException("conditionClasspathCatalog is required");
        }
        this.conditionClasspathCatalog = conditionClasspathCatalog;
    }

    public List<Class<? extends MarketCondition>> findAvailableConditions() {
        return conditionClasspathCatalog.findImplementations();
    }

    public String getDisplayName(Class<? extends MarketCondition> conditionClass) {
        String simpleName = conditionClass.getSimpleName();
        if (simpleName.endsWith("Condition")) {
            return simpleName.substring(0, simpleName.length() - "Condition".length());
        }
        return simpleName;
    }

}
