package forge.trigger;

import java.util.Objects;

public class TriggerResult {
    private final boolean triggered;
    private final TriggerDirection direction;

    private TriggerResult(boolean triggered, TriggerDirection direction) {
        this.triggered = triggered;
        this.direction = Objects.requireNonNull(direction, "direction is required");
        if (!triggered && direction != TriggerDirection.NONE) {
            throw new IllegalArgumentException("direction must be NONE when trigger condition is false");
        }
        if (triggered && direction == TriggerDirection.NONE) {
            throw new IllegalArgumentException("direction must be LONG or SHORT when trigger condition is true");
        }
    }

    public static TriggerResult triggered(TriggerDirection direction) {
        return new TriggerResult(true, direction);
    }

    public static TriggerResult notTriggered() {
        return new TriggerResult(false, TriggerDirection.NONE);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public TriggerDirection getDirection() {
        return direction;
    }
}
