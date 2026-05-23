package forge.study;

import forge.condition.FirstHourBreachCondition;

public class FirstHourBreachStudy implements MarketStudy {
    @Override
    public String getName() {
        return FirstHourBreachCondition.EVENT_NAME;
    }

    @Override
    public String getDisplayName() {
        return "First Hour Breach Frequency";
    }

    @Override
    public String getDescription() {
        return "Counts how often price breaches the first-hour RTH high or low after the first hour completes.";
    }
}
