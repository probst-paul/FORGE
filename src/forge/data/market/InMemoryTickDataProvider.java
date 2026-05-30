package forge.data.market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryTickDataProvider implements TickDataProvider {
    private final List<TradeTick> ticks;

    public InMemoryTickDataProvider() {
        /*
         * Intent: Create an empty in-memory tick provider for tests or placeholder workflows.
         * Precondition: None.
         * Returns: A constructed InMemoryTickDataProvider instance.
         * Postcondition: Provider contains no ticks.
         */
        this(Collections.emptyList());
    }

    public InMemoryTickDataProvider(List<TradeTick> ticks) {
        /*
         * Intent: Create an in-memory tick provider from a supplied tick list.
         * Precondition: Tick list must not be null.
         * Returns: A constructed InMemoryTickDataProvider instance.
         * Postcondition: Tick list is defensively copied and immutable.
         */
        if (ticks == null) {
            throw new IllegalArgumentException("ticks is required");
        }
        this.ticks = Collections.unmodifiableList(new ArrayList<>(ticks));
    }

    @Override
    public TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize) {
        /*
         * Intent: Open a batch reader over the stored in-memory ticks.
         * Precondition: Batch size must be positive.
         * Returns: TradeBatchReader that returns immutable batches.
         * Postcondition: Provider tick list is unchanged.
         */
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        return new InMemoryTradeBatchReader(ticks, batchSize);
    }

    @Override
    public long countTicks(List<ContractTradeWindow> windows) {
        return ticks.size();
    }

    private static class InMemoryTradeBatchReader implements TradeBatchReader {
        private final List<TradeTick> ticks;
        private final int batchSize;
        private int nextIndex;

        private InMemoryTradeBatchReader(List<TradeTick> ticks, int batchSize) {
            this.ticks = ticks;
            this.batchSize = batchSize;
        }

        @Override
        public List<TradeTick> readNextBatch() {
            /*
             * Intent: Return the next immutable batch from the in-memory tick list.
             * Precondition: Reader must have been created with a positive batch size.
             * Returns: Next tick batch or empty list when exhausted.
             * Postcondition: Reader cursor advances by the returned batch size.
             */
            if (nextIndex >= ticks.size()) {
                return Collections.emptyList();
            }
            int endIndex = Math.min(nextIndex + batchSize, ticks.size());
            List<TradeTick> batch = new ArrayList<>(ticks.subList(nextIndex, endIndex));
            nextIndex = endIndex;
            return Collections.unmodifiableList(batch);
        }
    }
}
