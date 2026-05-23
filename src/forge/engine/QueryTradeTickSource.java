package forge.engine;

import forge.data.market.ContractTradeWindow;
import forge.data.market.TradeBatchReader;

import java.util.List;

public interface QueryTradeTickSource {
    TradeBatchReader openTradeBatchReader(List<ContractTradeWindow> windows, int batchSize);

    long countTradeTicks(List<ContractTradeWindow> windows);
}
