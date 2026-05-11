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
        void storesTargetAndStopPrices() {
            TargetResult result = new TargetResult(5002, 4998);

            assertEquals(5002, result.getTargetPrice());
            assertEquals(4998, result.getStopPrice());
        }

        @Test
        void rejectsInvalidPrices() {
            assertThrows(IllegalArgumentException.class, () -> new TargetResult(0, 4998));
            assertThrows(IllegalArgumentException.class, () -> new TargetResult(5002, 0));
        }
    }
}
