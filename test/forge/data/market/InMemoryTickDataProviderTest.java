package forge.data.market;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
class InMemoryTickDataProviderTest {
    @Nested
    class ReadNextBatch {
        @Test
        void returnsTicksInConfiguredBatchSizes() {
            InMemoryTickDataProvider provider = new InMemoryTickDataProvider(List.of(
                    tick(1),
                    tick(2),
                    tick(3)
            ));

            TradeBatchReader reader = provider.openReader(List.of(), 2);

            List<TradeTick> firstBatch = reader.readNextBatch();
            List<TradeTick> secondBatch = reader.readNextBatch();
            List<TradeTick> thirdBatch = reader.readNextBatch();

            assertEquals(2, firstBatch.size());
            assertEquals(1, firstBatch.get(0).getScidRecordIndex());
            assertEquals(2, firstBatch.get(1).getScidRecordIndex());
            assertEquals(1, secondBatch.size());
            assertEquals(3, secondBatch.get(0).getScidRecordIndex());
            assertTrue(thirdBatch.isEmpty());
        }
    }

    private TradeTick tick(long scidRecordIndex) {
        return new TradeTick(
                "ESU25",
                Instant.parse("2025-08-01T14:30:00Z").plusSeconds(scidRecordIndex),
                24000 + scidRecordIndex,
                23999 + scidRecordIndex,
                24001 + scidRecordIndex,
                1,
                1,
                scidRecordIndex
        );
    }
}
