package forge.cli;

import forge.app.UserInput;
import forge.config.RiskSettings;

public class RiskSettingsSelectionService {
    public RiskSettings readRiskSettings(UserInput input) {
        double riskPerTrade = input.readDouble("Risk per trade");
        double maxDailyLoss = input.readDouble("Max daily loss");
        return new RiskSettings(riskPerTrade, maxDailyLoss);
    }
}
