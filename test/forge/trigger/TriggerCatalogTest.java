package forge.trigger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class TriggerCatalogTest {
    private final TriggerCatalog catalog = new TriggerCatalog();

    @Nested
    class FindAvailableTriggers {
        @Test
        void includesConcreteTradeTriggers() {
            List<Class<? extends TradeTrigger>> triggers = catalog.findAvailableTriggers();

            assertTrue(triggers.contains(OrderFlowExhaustionTrigger.class));
            assertTrue(triggers.contains(PriceCrossoverTrigger.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void removesTriggerSuffix() {
            assertEquals("OrderFlowExhaustion", catalog.getDisplayName(OrderFlowExhaustionTrigger.class));
            assertEquals("PriceCrossover", catalog.getDisplayName(PriceCrossoverTrigger.class));
        }
    }
}
