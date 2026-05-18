package forge.stop;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StopResultTest {
    @Nested
    class Stopped {
        @Test
        void createsStoppedResult() {
            StopResult result = StopResult.stopped(StopReason.PRICE, 100);

            assertTrue(result.isStopped());
            assertEquals(StopReason.PRICE, result.getReason());
            assertEquals(100, result.getExitPriceTicks());
        }

        @Test
        void rejectsNoneReason() {
            assertThrows(IllegalArgumentException.class, () -> StopResult.stopped(StopReason.NONE, 100));
        }

        @Test
        void rejectsNonPositiveExitPriceTicks() {
            assertThrows(IllegalArgumentException.class, () -> StopResult.stopped(StopReason.PRICE, 0));
        }
    }

    @Nested
    class NotStopped {
        @Test
        void createsNotStoppedResult() {
            StopResult result = StopResult.notStopped();

            assertFalse(result.isStopped());
            assertEquals(StopReason.NONE, result.getReason());
            assertEquals(0, result.getExitPriceTicks());
        }
    }
}
