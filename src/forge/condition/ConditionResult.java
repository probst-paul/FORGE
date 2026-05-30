package forge.condition;

import java.util.Objects;

public class ConditionResult {
    private final boolean conditioned;
    private final ConditionDirection direction;

    private ConditionResult(boolean conditioned, ConditionDirection direction) {
        /*
         * Intent: Normalize and validate the relationship between condition state and trade direction.
         * Precondition: Direction must be non-null and consistent with whether the condition is true.
         * Returns: A constructed ConditionResult instance.
         * Postcondition: Result is immutable and internally consistent.
         */
        this.conditioned = conditioned;
        this.direction = Objects.requireNonNull(direction, "direction is required");
        if (!conditioned && direction != ConditionDirection.NONE) {
            throw new IllegalArgumentException("direction must be NONE when market condition is false");
        }
        if (conditioned && direction == ConditionDirection.NONE) {
            throw new IllegalArgumentException("direction must be LONG or SHORT when market condition is true");
        }
    }

    public static ConditionResult conditioned(ConditionDirection direction) {
        /*
         * Intent: Create a true condition result with a trade direction.
         * Precondition: Direction must be LONG or SHORT.
         * Returns: ConditionResult representing an active condition.
         * Postcondition: Returned result is immutable.
         */
        return new ConditionResult(true, direction);
    }

    public static ConditionResult notConditioned() {
        /*
         * Intent: Create a false condition result with no trade direction.
         * Precondition: None.
         * Returns: ConditionResult representing no active condition.
         * Postcondition: Returned result is immutable.
         */
        return new ConditionResult(false, ConditionDirection.NONE);
    }

    public boolean isConditioned() {
        return conditioned;
    }

    public ConditionDirection getDirection() {
        return direction;
    }
}
