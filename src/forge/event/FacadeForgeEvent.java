package forge.event;

import java.util.List;

public class FacadeForgeEvent {
    private static final FacadeForgeEvent THE_INSTANCE = new FacadeForgeEvent();

    private final EventBuildService eventBuildService;
    private final ForgeEventAccess access = new ForgeEventAccess();

    public static FacadeForgeEvent getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeEvent() {
        this(new EventBuildService());
    }

    public FacadeForgeEvent(EventBuildService eventBuildService) {
        if (eventBuildService == null) {
            throw new IllegalArgumentException("eventBuildService is required");
        }
        this.eventBuildService = eventBuildService;
    }

    public ForgeEventAccess forgeEventAccess() {
        return access;
    }

    public class ForgeEventAccess {
        public List<String> getSupportedEventNames() {
            return eventBuildService.getSupportedEventNames();
        }
    }
}
