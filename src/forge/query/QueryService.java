package forge.query;

import forge.event.FirstHourBreachEvent;

import java.util.List;

public class QueryService {
    public List<String> getSupportedQueryEventNames() {
        return List.of(FirstHourBreachEvent.EVENT_NAME);
    }
}
