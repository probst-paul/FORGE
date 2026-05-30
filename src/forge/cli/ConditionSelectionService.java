package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.config.MarketConditionOptions;
import forge.strategy.StrategyConfigurationProfile;
import forge.condition.PriceCrossoverCondition;
import forge.condition.ConditionDirection;
import forge.condition.MarketCondition;
import forge.condition.FacadeForgeCondition;

import java.util.List;
import java.util.Map;

public class ConditionSelectionService {
    private final FacadeForgeCondition facadeCondition;

    public ConditionSelectionService(FacadeForgeCondition facadeCondition) {
        /*
         * Intent: Create a CLI condition selection service backed by the condition facade.
         * Precondition: Condition facade should be available.
         * Returns: A constructed ConditionSelectionService instance.
         * Postcondition: Future condition choices query condition metadata through the supplied facade.
         */
        this.facadeCondition = facadeCondition;
    }

    /*
     * Intent: Let the user select from all available market conditions.
     * Precondition: At least one condition must be registered in the condition facade.
     * Returns: Selected MarketCondition class.
     * Postcondition: Input is consumed until a valid selection is made or the user quits.
     */
    public Class<? extends MarketCondition> selectCondition(UserInput input, UserOutput output) {
        List<Class<? extends MarketCondition>> conditions = facadeCondition.forgeConditionAccess().findAvailableConditions();
        if (conditions.isEmpty()) {
            throw new IllegalStateException("No market conditions are available");
        }

        output.printLine("Available market conditions:");
        for (int i = 0; i < conditions.size(); i++) {
            output.printLine((i + 1) + ". " + facadeCondition.forgeConditionAccess().getDisplayName(conditions.get(i)));
        }

        while (true) {
            int selectedIndex = input.readInt("Select market condition") - 1;
            if (selectedIndex >= 0 && selectedIndex < conditions.size()) {
                return conditions.get(selectedIndex);
            }
            output.printLine("Selected market condition is not available. Please select an available condition, or enter 'quit' to exit program.");
        }
    }

    /*
     * Intent: Select a market condition while honoring the selected strategy's configuration profile.
     * Precondition: Strategy profile must define a default condition and any allowed condition choices.
     * Returns: Default condition when selection is locked, otherwise the user's selected allowed condition.
     * Postcondition: User cannot select a condition outside the strategy profile.
     */
    public Class<? extends MarketCondition> selectCondition(
            UserInput input,
            UserOutput output,
            StrategyConfigurationProfile strategyProfile
    ) {
        List<Class<? extends MarketCondition>> conditions = strategyProfile.getAllowedConditions();
        if (!strategyProfile.isConditionSelectionAllowed()) {
            Class<? extends MarketCondition> condition = strategyProfile.getDefaultCondition();
            output.printLine("Using market condition: " + getDisplayName(condition));
            return condition;
        }

        output.printLine("Available market conditions:");
        for (int i = 0; i < conditions.size(); i++) {
            Class<? extends MarketCondition> condition = conditions.get(i);
            String defaultMarker = condition.equals(strategyProfile.getDefaultCondition()) ? " (default)" : "";
            output.printLine((i + 1) + ". " + getDisplayName(condition) + defaultMarker);
        }

        while (true) {
            int selectedIndex = input.readInt("Select market condition") - 1;
            if (selectedIndex >= 0 && selectedIndex < conditions.size()) {
                return conditions.get(selectedIndex);
            }
            output.printLine("Selected market condition is not available for this strategy. Please select an available condition, or enter 'quit' to exit program.");
        }
    }

    public String getDisplayName(Class<? extends MarketCondition> condition) {
        return facadeCondition.forgeConditionAccess().getDisplayName(condition);
    }

    public boolean hasConfigurableOptions(Class<? extends MarketCondition> condition) {
        return PriceCrossoverCondition.class.equals(condition);
    }

    public MarketConditionOptions createDefaultConditionOptions(Class<? extends MarketCondition> condition) {
        return facadeCondition.forgeConditionAccess().createConditionOptions(condition);
    }

    /*
     * Intent: Read CLI configuration for a selected market condition when that condition has options.
     * Precondition: Condition class must be supported by the condition facade.
     * Returns: MarketConditionOptions for the selected condition.
     * Postcondition: Invalid option values are rejected and reprompted without changing data state.
     */
    public MarketConditionOptions readConditionOptions(
            UserInput input,
            UserOutput output,
            Class<? extends MarketCondition> condition
    ) {
        if (!hasConfigurableOptions(condition)) {
            return facadeCondition.forgeConditionAccess().createConditionOptions(condition);
        }

        while (true) {
            try {
                ConditionDirection direction = readDirection(input, output);
                long priceThresholdTicks = input.readLong("Price threshold ticks");
                if (priceThresholdTicks <= 0) {
                    throw new IllegalArgumentException("priceThresholdTicks must be greater than zero");
                }
                return facadeCondition.forgeConditionAccess().createConditionOptions(
                        condition,
                        Map.of(
                                "direction", direction.name(),
                                "priceThresholdTicks", Long.toString(priceThresholdTicks)
                        )
                );
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter valid condition settings, or enter 'quit' to exit program.");
            }
        }
    }

    /*
     * Intent: Read the direction option for a configurable price-crossover condition.
     * Precondition: User must enter one of the displayed menu choices.
     * Returns: LONG or SHORT condition direction.
     * Postcondition: Invalid selection raises an exception for the caller's reprompt loop.
     */
    private ConditionDirection readDirection(UserInput input, UserOutput output) {
        output.printLine("Condition direction:");
        output.printLine("1. Long");
        output.printLine("2. Short");
        int selectedDirection = input.readInt("Select condition direction");
        if (selectedDirection == 1) {
            return ConditionDirection.LONG;
        }
        if (selectedDirection == 2) {
            return ConditionDirection.SHORT;
        }
        throw new IllegalArgumentException("Selected condition direction is not available");
    }
}
