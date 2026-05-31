package forge.event;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class EventCatalogTest {
    private final EventCatalog catalog = new EventCatalog();

    @Nested
    class FindAvailableEvents {
        @Test
        void includesConcreteMarketEvents() {
            List<Class<? extends MarketEvent>> events = catalog.findAvailableEvents();

            assertTrue(events.contains(OrderFlowExhaustionEvent.class));
            assertTrue(events.contains(PriceCrossoverEvent.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void removesEventSuffix() {
            assertEquals("OrderFlowExhaustion", catalog.getDisplayName(OrderFlowExhaustionEvent.class));
            assertEquals("PriceCrossover", catalog.getDisplayName(PriceCrossoverEvent.class));
        }
    }
}
