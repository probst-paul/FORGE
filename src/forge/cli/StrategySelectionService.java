package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.strategy.FacadeForgeStrategy;
import forge.strategy.TradingStrategy;

import java.util.List;

public class StrategySelectionService {
    private final FacadeForgeStrategy facadeStrategy;

    public StrategySelectionService(FacadeForgeStrategy facadeStrategy) {
        this.facadeStrategy = facadeStrategy;
    }

    public Class<? extends TradingStrategy> selectStrategy(UserInput input, UserOutput output) {
        List<Class<? extends TradingStrategy>> strategies = facadeStrategy.forgeStrategyAccess().findAvailableStrategies();
        if (strategies.isEmpty()) {
            throw new IllegalStateException("No trading strategies are available");
        }

        output.printLine("Available strategies:");
        for (int i = 0; i < strategies.size(); i++) {
            output.printLine((i + 1) + ". " + facadeStrategy.forgeStrategyAccess().getDisplayName(strategies.get(i)));
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
}
