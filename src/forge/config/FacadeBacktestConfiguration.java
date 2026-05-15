package forge.config;

import forge.execution.OrderType;

import java.time.LocalDate;
import java.util.List;

public class FacadeBacktestConfiguration {
    public BacktestRequest createBacktestRequest(
            String strategyName,
            List<String> instruments,
            LocalDate startDate,
            LocalDate endDate,
            String triggerName,
            RiskSettings riskSettings,
            TargetSettings targetSettings
    ) {
        return createBacktestRequest(
                new StrategyOptions(strategyName),
                instruments,
                startDate,
                endDate,
                new TradeTriggerOptions(triggerName),
                riskSettings,
                targetSettings,
                defaultOrderSettings()
        );
    }

    public BacktestRequest createBacktestRequest(
            StrategyOptions strategyOptions,
            List<String> instruments,
            LocalDate startDate,
            LocalDate endDate,
            TradeTriggerOptions tradeTriggerOptions,
            RiskSettings riskSettings,
            TargetSettings targetSettings,
            OrderSettings orderSettings
    ) {
        return new BacktestRequest(
                strategyOptions,
                instruments,
                startDate,
                endDate,
                tradeTriggerOptions,
                riskSettings,
                targetSettings,
                orderSettings
        );
    }

    public OrderSettings defaultOrderSettings() {
        return new OrderSettings(OrderType.MARKET, 1, 0, 0);
    }
}
