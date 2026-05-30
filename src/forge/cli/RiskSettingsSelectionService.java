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
                double riskPerTrade = input.readDouble("Risk per trade");
                double maxDailyLoss = input.readDouble("Max daily loss");
                return new RiskSettings(riskPerTrade, maxDailyLoss);
            } catch (IllegalArgumentException exception) {
                output.printLine(exception.getMessage() + ". Please enter valid risk settings, or enter 'quit' to exit program.");
            }
        }
    }
}
