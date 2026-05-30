package forge.condition;

import forge.util.ClasspathCatalog;

import java.util.List;

public class ConditionCatalog {
    private static final String CONDITION_PACKAGE = "forge.condition";
    private final ClasspathCatalog<MarketCondition> conditionClasspathCatalog;

    public ConditionCatalog() {
        /*
         * Intent: Create a catalog that discovers condition implementations from the condition package.
         * Precondition: Condition classes must be on the application classpath.
         * Returns: A constructed ConditionCatalog instance.
         * Postcondition: Catalog can discover MarketCondition implementations.
         */
        this(new ClasspathCatalog<>(CONDITION_PACKAGE, MarketCondition.class));
    }

    public ConditionCatalog(ClasspathCatalog<MarketCondition> conditionClasspathCatalog) {
        /*
         * Intent: Create a catalog with an explicit classpath discovery helper.
         * Precondition: Discovery helper must not be null.
         * Returns: A constructed ConditionCatalog instance.
         * Postcondition: Future condition discovery delegates to the supplied helper.
         */
        if (conditionClasspathCatalog == null) {
            throw new IllegalArgumentException("conditionClasspathCatalog is required");
        }
        this.conditionClasspathCatalog = conditionClasspathCatalog;
    }

    public List<Class<? extends MarketCondition>> findAvailableConditions() {
        /*
         * Intent: Discover available market condition implementations.
         * Precondition: Classpath discovery helper must be configured for MarketCondition.
         * Returns: List of discovered condition classes.
         * Postcondition: Catalog state is unchanged.
         */
        return conditionClasspathCatalog.findImplementations();
    }

    public String getDisplayName(Class<? extends MarketCondition> conditionClass) {
        /*
         * Intent: Convert a condition class name into a user-facing display name.
         * Precondition: Condition class must not be null.
         * Returns: Simple class name with the Condition suffix removed when present.
         * Postcondition: Class metadata and catalog state are unchanged.
         */
        String simpleName = conditionClass.getSimpleName();
        if (simpleName.endsWith("Condition")) {
            return simpleName.substring(0, simpleName.length() - "Condition".length());
        }
        return simpleName;
    }

}
