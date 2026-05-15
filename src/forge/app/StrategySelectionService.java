package forge.app;

import forge.strategy.StrategyCatalog;
import forge.strategy.TradingStrategy;

import java.util.List;

public class StrategySelectionService {
    private final StrategyCatalog strategyCatalog;

    public StrategySelectionService(StrategyCatalog strategyCatalog) {
        this.strategyCatalog = strategyCatalog;
    }

    public Class<? extends TradingStrategy> selectStrategy(UserInput input, UserOutput output) {
        List<Class<? extends TradingStrategy>> strategies = strategyCatalog.findAvailableStrategies();
        if (strategies.isEmpty()) {
            throw new IllegalStateException("No trading strategies are available");
        }

        output.printLine("Available strategies:");
        for (int i = 0; i < strategies.size(); i++) {
            output.printLine((i + 1) + ". " + strategyCatalog.getDisplayName(strategies.get(i)));
        }

        int selectedIndex = input.readInt("Select strategy") - 1;
        if (selectedIndex < 0 || selectedIndex >= strategies.size()) {
            throw new IllegalArgumentException("Selected strategy is not available");
        }
        return strategies.get(selectedIndex);
    }

    public String getDisplayName(Class<? extends TradingStrategy> strategy) {
        return strategyCatalog.getDisplayName(strategy);
    }
}
