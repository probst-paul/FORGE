package forge.data.market;

import java.util.List;

public interface TradeBatchReader {
    List<TradeTick> readNextBatch();
}
