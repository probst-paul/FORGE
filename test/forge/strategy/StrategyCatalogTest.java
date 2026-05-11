package forge.strategy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class StrategyCatalogTest {
    private final StrategyCatalog catalog = new StrategyCatalog();

    @Nested
    class FindAvailableStrategies {
        @Test
        void includesConcreteTradingStrategies() {
            List<Class<? extends TradingStrategy>> strategies = catalog.findAvailableStrategies();

            assertTrue(strategies.contains(RangeBreakoutStrategy.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void removesStrategySuffix() {
            assertEquals("RangeBreakout", catalog.getDisplayName(RangeBreakoutStrategy.class));
        }
    }
}
