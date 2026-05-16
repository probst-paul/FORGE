package forge.app;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
class ImportProgressTest {
    @Nested
    class Completion {
        @Test
        void calculatesCompletionRatioAndPercent() {
            ImportProgress progress = new ImportProgress("ESU25", 25, 100);

            assertEquals(0.25, progress.getCompletionRatio());
            assertEquals(25, progress.getCompletionPercent());
        }

        @Test
        void treatsEmptyImportsAsComplete() {
            ImportProgress progress = new ImportProgress("ESU25", 0, 0);

            assertEquals(1.0, progress.getCompletionRatio());
            assertEquals(100, progress.getCompletionPercent());
        }

        @Test
        void rejectsProcessedRecordsPastTotalRecords() {
            assertThrows(IllegalArgumentException.class, () -> new ImportProgress("ESU25", 2, 1));
        }
    }
}
