package forge.condition;

public class FirstHourBreachCondition implements ConditionDefinition {
    public static final String EVENT_NAME = "FIRST_HOUR_BREACH";
    public static final int EVENT_VERSION = 1;

    @Override
    public String getName() {
        return EVENT_NAME;
    }

    @Override
    public int getVersion() {
        return EVENT_VERSION;
    }
}
