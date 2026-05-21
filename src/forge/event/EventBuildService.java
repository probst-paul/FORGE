package forge.event;

import java.util.List;

public class EventBuildService {
    public List<String> getSupportedEventNames() {
        return List.of(FirstHourBreachEvent.EVENT_NAME);
    }
}
