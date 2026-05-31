package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;
import forge.config.RiskSettings;

public class RiskSettingsSelectionService {
    /*
     * Intent: Read risk settings from the CLI and validate them through the RiskSettings model.
     * Precondition: User input must eventually provide valid numeric risk values.
     * Returns: RiskSettings accepted by the config layer.
     * Postcondition: Invalid values are rejected and reprompted without changing app state.
     */
    public RiskSettings readRiskSettings(UserInput input, UserOutput output) {
        while (true) {
            try {
                boolean perTradeRiskEnabled = readEnabled(input, output, "Enable per-trade risk limit? (y/n)");
                double riskPerTrade = perTradeRiskEnabled
                        ? input.readDouble("Risk per trade")
                        : 0.0;
                boolean dailyRiskEnabled = readEnabled(input, output, "Enable daily risk limit? (y/n)");
                double maxDailyLoss = dailyRiskEnabled
                        ? input.readDouble("Max daily loss")
                        : 0.0;
                return new RiskSettings(perTradeRiskEnabled, riskPerTrade, dailyRiskEnabled, maxDailyLoss);
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter valid risk settings, or enter 'quit' to exit program.");
            }
        }
    }

    private boolean readEnabled(UserInput input, UserOutput output, String label) {
        while (true) {
            String value = input.readString(label).trim();
            if ("y".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) {
                return true;
            }
            if ("n".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)) {
                return false;
            }
            output.printLine("Please enter y or n, or enter 'quit' to exit program.");
        }
    }
}
