package forge.event;

import forge.data.market.TradeTick;
import forge.feature.SessionRangeFeature;

import java.util.Collection;
import java.util.List;

public class EventBuildService {
    private final FirstHourBreachEventDetector firstHourBreachEventDetector;

    public EventBuildService() {
        this(new FirstHourBreachEventDetector());
    }

    public EventBuildService(FirstHourBreachEventDetector firstHourBreachEventDetector) {
        if (firstHourBreachEventDetector == null) {
            throw new IllegalArgumentException("firstHourBreachEventDetector is required");
        }
        this.firstHourBreachEventDetector = firstHourBreachEventDetector;
    }

    public List<String> getSupportedEventNames() {
        return List.of(FirstHourBreachEvent.EVENT_NAME);
    }

    public List<MarketEvent> detectFirstHourBreachEvents(
            Collection<SessionRangeFeature> sessionRangeFeatures,
            Collection<TradeTick> ticks
    ) {
        return firstHourBreachEventDetector.detect(sessionRangeFeatures, ticks);
    }
}
