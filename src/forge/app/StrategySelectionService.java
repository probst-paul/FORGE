package forge.app;

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

        int selectedIndex = input.readInt("Select strategy") - 1;
        if (selectedIndex < 0 || selectedIndex >= strategies.size()) {
            throw new IllegalArgumentException("Selected strategy is not available");
        }
        return strategies.get(selectedIndex);
    }

    public String getDisplayName(Class<? extends TradingStrategy> strategy) {
        return facadeStrategy.forgeStrategyAccess().getDisplayName(strategy);
    }
}
