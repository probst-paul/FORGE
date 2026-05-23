package forge.condition;

import java.util.Objects;

public class ConditionResult {
    private final boolean conditioned;
    private final ConditionDirection direction;

    private ConditionResult(boolean conditioned, ConditionDirection direction) {
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
        return new ConditionResult(true, direction);
    }

    public static ConditionResult notConditioned() {
        return new ConditionResult(false, ConditionDirection.NONE);
    }

    public boolean isConditioned() {
        return conditioned;
    }

    public ConditionDirection getDirection() {
        return direction;
    }
}
