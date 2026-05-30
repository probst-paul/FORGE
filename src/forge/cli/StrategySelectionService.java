package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.strategy.FacadeForgeStrategy;
import forge.strategy.StrategyConfigurationProfile;
import forge.strategy.TradingStrategy;

import java.util.List;

public class StrategySelectionService {
    private final FacadeForgeStrategy facadeStrategy;

    public StrategySelectionService(FacadeForgeStrategy facadeStrategy) {
        /*
         * Intent: Create a CLI strategy selection service backed by the strategy facade.
         * Precondition: Strategy facade should be available.
         * Returns: A constructed StrategySelectionService instance.
         * Postcondition: Future strategy choices query strategy metadata through the supplied facade.
         */
        this.facadeStrategy = facadeStrategy;
    }

    /*
     * Intent: Let the user choose one available trading strategy.
     * Precondition: At least one strategy must be registered in the strategy facade.
     * Returns: Selected TradingStrategy class.
     * Postcondition: Input is consumed until a valid selection is made or the user quits.
     */
    public Class<? extends TradingStrategy> selectStrategy(UserInput input, UserOutput output) {
        List<Class<? extends TradingStrategy>> strategies = facadeStrategy.forgeStrategyAccess().findAvailableStrategies();
        if (strategies.isEmpty()) {
            throw new IllegalStateException("No trading strategies are available");
        }

        output.printLine("Available strategies:");
        for (int i = 0; i < strategies.size(); i++) {
            Class<? extends TradingStrategy> strategy = strategies.get(i);
            output.printLine((i + 1) + ". " + facadeStrategy.forgeStrategyAccess().getDisplayName(strategy));
            output.printLine("   " + facadeStrategy.forgeStrategyAccess().getDescription(strategy));
        }

        while (true) {
            int selectedIndex = input.readInt("Select strategy") - 1;
            if (selectedIndex >= 0 && selectedIndex < strategies.size()) {
                return strategies.get(selectedIndex);
            }
            output.printLine("Selected strategy is not available. Please select an available strategy, or enter 'quit' to exit program.");
        }
    }

    public String getDisplayName(Class<? extends TradingStrategy> strategy) {
        return facadeStrategy.forgeStrategyAccess().getDisplayName(strategy);
    }

    public String getDescription(Class<? extends TradingStrategy> strategy) {
        return facadeStrategy.forgeStrategyAccess().getDescription(strategy);
    }

    public StrategyConfigurationProfile getConfigurationProfile(Class<? extends TradingStrategy> strategy) {
        return facadeStrategy.forgeStrategyAccess().getConfigurationProfile(strategy);
    }
}
