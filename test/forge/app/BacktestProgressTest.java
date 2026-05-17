package forge.app;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class BacktestProgressTest {
    @Nested
    class Completion {
        @Test
        void calculatesCompletionRatioAndPercent() {
            BacktestProgress progress = new BacktestProgress(25, 100);

            assertEquals(0.25, progress.getCompletionRatio());
            assertEquals(25, progress.getCompletionPercent());
        }

        @Test
        void treatsEmptyBacktestsAsComplete() {
            BacktestProgress progress = new BacktestProgress(0, 0);

            assertEquals(1.0, progress.getCompletionRatio());
            assertEquals(100, progress.getCompletionPercent());
        }

        @Test
        void rejectsProcessedTicksPastTotalTicks() {
            assertThrows(IllegalArgumentException.class, () -> new BacktestProgress(2, 1));
        }
    }
}
