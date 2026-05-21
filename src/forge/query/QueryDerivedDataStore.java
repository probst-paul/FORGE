package forge.query;

import forge.data.market.ContractTradeWindow;
import forge.event.MarketEvent;
import forge.feature.SessionRangeFeature;

import java.util.Collection;
import java.util.List;

public interface QueryDerivedDataStore {
    boolean areSessionRangesBuilt(List<ContractTradeWindow> windows);

    List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows);

    void saveSessionRanges(Collection<SessionRangeFeature> sessionRangeFeatures);

    void markSessionRangesBuilt(List<ContractTradeWindow> windows);

    boolean areMarketEventsBuilt(List<ContractTradeWindow> windows, String eventName);

    List<MarketEvent> loadMarketEvents(List<ContractTradeWindow> windows, String eventName);

    void saveMarketEvents(Collection<MarketEvent> marketEvents);

    void markMarketEventsBuilt(List<ContractTradeWindow> windows, String eventName);
}
