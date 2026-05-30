package forge.condition;

public class FirstHourBreachCondition implements ConditionDefinition {
    public static final String EVENT_NAME = "FIRST_HOUR_BREACH";
    public static final int EVENT_VERSION = 1;

    @Override
    public String getName() {
        /*
         * Intent: Provide the stable storage/query name for first-hour breach occurrences.
         * Precondition: None.
         * Returns: First-hour breach event name.
         * Postcondition: Definition state is unchanged.
         */
        return EVENT_NAME;
    }

    @Override
    public int getVersion() {
        /*
         * Intent: Identify the schema/logic version for stored first-hour breach occurrences.
         * Precondition: None.
         * Returns: Positive event version.
         * Postcondition: Definition state is unchanged.
         */
        return EVENT_VERSION;
    }
}
