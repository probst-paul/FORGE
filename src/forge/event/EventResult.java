package forge.event;

import java.util.Objects;

public class EventResult {
    private final boolean conditioned;
    private final EventDirection direction;

    private EventResult(boolean conditioned, EventDirection direction) {
        /*
         * Intent: Normalize and validate the relationship between condition state and trade direction.
         * Precondition: Direction must be non-null and consistent with whether the condition is true.
         * Returns: A constructed EventResult instance.
         * Postcondition: Result is immutable and internally consistent.
         */
        this.conditioned = conditioned;
        this.direction = Objects.requireNonNull(direction, "direction is required");
        if (!conditioned && direction != EventDirection.NONE) {
            throw new IllegalArgumentException("direction must be NONE when market event is false");
        }
        if (conditioned && direction == EventDirection.NONE) {
            throw new IllegalArgumentException("direction must be LONG or SHORT when market event is true");
        }
    }

    public static EventResult conditioned(EventDirection direction) {
        /*
         * Intent: Create a true condition result with a trade direction.
         * Precondition: Direction must be LONG or SHORT.
         * Returns: EventResult representing an active condition.
         * Postcondition: Returned result is immutable.
         */
        return new EventResult(true, direction);
    }

    public static EventResult notConditioned() {
        /*
         * Intent: Create a false condition result with no trade direction.
         * Precondition: None.
         * Returns: EventResult representing no active condition.
         * Postcondition: Returned result is immutable.
         */
        return new EventResult(false, EventDirection.NONE);
    }

    public boolean isConditioned() {
        return conditioned;
    }

    public EventDirection getDirection() {
        return direction;
    }
}
