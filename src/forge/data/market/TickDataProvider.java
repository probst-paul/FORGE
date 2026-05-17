package forge.data.market;

import java.util.List;

public interface TickDataProvider {
    TradeBatchReader openReader(List<ContractTradeWindow> windows, int batchSize);
}
