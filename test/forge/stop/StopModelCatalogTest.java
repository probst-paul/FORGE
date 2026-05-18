package forge.stop;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopModelCatalogTest {
    private final StopModelCatalog catalog = new StopModelCatalog();

    @Nested
    class FindAvailableStopModels {
        @Test
        void includesImplementedStopModels() {
            List<Class<? extends StopModel>> stopModels = catalog.findAvailableStopModels();

            assertTrue(stopModels.contains(PriceBasedStop.class));
            assertTrue(stopModels.contains(TimeBasedStop.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void returnsFriendlyNames() {
            assertEquals("Price Based", catalog.getDisplayName(PriceBasedStop.class));
            assertEquals("Time Based", catalog.getDisplayName(TimeBasedStop.class));
        }
    }
}
