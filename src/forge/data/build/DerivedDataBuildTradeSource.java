package forge.data.build;

import forge.data.market.ContractTradeWindow;
import forge.data.market.TradeBatchReader;

import java.util.List;

public interface DerivedDataBuildTradeSource {
    TradeBatchReader openTradeBatchReader(List<ContractTradeWindow> windows, int batchSize);

    long countTradeTicks(List<ContractTradeWindow> windows);
}
