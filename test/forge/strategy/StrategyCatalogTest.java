package forge.strategy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import forge.event.OrderFlowExhaustionEvent;
import forge.event.PriceCrossoverEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            assertTrue(strategies.contains(OpeningRangeContinuationStrategy.class));
        }
    }

    @Nested
    class GetDisplayName {
        @Test
        void removesStrategySuffix() {
            assertEquals("RangeBreakout", catalog.getDisplayName(RangeBreakoutStrategy.class));
            assertEquals("OpeningRangeContinuation", catalog.getDisplayName(OpeningRangeContinuationStrategy.class));
        }
    }

    @Nested
    class GetDescription {
        @Test
        void returnsShortUserFacingStrategyDescriptions() {
            assertTrue(catalog.getDescription(RangeBreakoutStrategy.class).contains("break"));
            assertTrue(catalog.getDescription(OpeningRangeContinuationStrategy.class).contains("first-hour"));
        }
    }

    @Nested
    class GetConfigurationProfile {
        @Test
        void rangeBreakoutDefinesCompatibleEvents() {
            StrategyConfigurationProfile profile = catalog.getConfigurationProfile(RangeBreakoutStrategy.class);

            assertEquals(RangeBreakoutStrategy.class, profile.getStrategyClass());
            assertEquals(List.of(OrderFlowExhaustionEvent.class, PriceCrossoverEvent.class), profile.getAllowedEvents());
            assertEquals(OrderFlowExhaustionEvent.class, profile.getDefaultEvent());
            assertTrue(profile.isEventSelectionAllowed());
        }

        @Test
        void openingRangeContinuationDefinesInternalEventProfile() {
            StrategyConfigurationProfile profile = catalog.getConfigurationProfile(OpeningRangeContinuationStrategy.class);

            assertEquals(OpeningRangeContinuationStrategy.class, profile.getStrategyClass());
            assertEquals(List.of(PriceCrossoverEvent.class), profile.getAllowedEvents());
            assertEquals(PriceCrossoverEvent.class, profile.getDefaultEvent());
            assertFalse(profile.isEventSelectionAllowed());
        }
    }
}
