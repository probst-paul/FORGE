package forge.engine.concurrency;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineJobRunnerTest {
    @Nested
    class RunAll {
        @Test
        void returnsGenericResultsInSubmissionOrder() {
            EngineJobRunner runner = new EngineJobRunner("test-engine-job", 2);

            List<String> results = runner.runAll(List.of(
                    () -> "event-statistics",
                    () -> "backtest",
                    () -> "report"
            ));

            assertEquals(List.of("event-statistics", "backtest", "report"), results);
        }

        @Test
        void acceptsEmptyJobLists() {
            EngineJobRunner runner = new EngineJobRunner("test-engine-job", 2);

            List<Integer> results = runner.runAll(List.of());

            assertTrue(results.isEmpty());
        }
    }
}
