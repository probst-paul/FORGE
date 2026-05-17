package forge.data.market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryTickDataProvider implements TickDataProvider {
    private final List<TradeTick> ticks;

    public InMemoryTickDataProvider() {
        this(Collections.emptyList());
    }

    public InMemoryTickDataProvider(List<TradeTick> ticks) {
        if (ticks == null) {
            throw new IllegalArgumentException("ticks is required");
        }
        this.ticks = Collections.unmodifiableList(new ArrayList<>(ticks));
    }

    @Override
    public TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        return new InMemoryTradeBatchReader(ticks, batchSize);
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
