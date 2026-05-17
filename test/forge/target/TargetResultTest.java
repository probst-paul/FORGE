package forge.target;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class TargetResultTest {
    @Nested
    class Constructor {
        @Test
        void storesTargetAndStopPriceTicks() {
            TargetResult result = new TargetResult(20008, 19992);

            assertEquals(20008, result.getTargetPriceTicks());
            assertEquals(19992, result.getStopPriceTicks());
            assertEquals(5002.0, result.getTargetPrice(0.25));
            assertEquals(4998.0, result.getStopPrice(0.25));
        }

        @Test
        void rejectsInvalidPriceTicks() {
            assertThrows(IllegalArgumentException.class, () -> new TargetResult(0, 4998));
            assertThrows(IllegalArgumentException.class, () -> new TargetResult(5002, 0));
            assertThrows(IllegalArgumentException.class, () -> new TargetResult(5002, 4998).getTargetPrice(0));
        }
    }
}
