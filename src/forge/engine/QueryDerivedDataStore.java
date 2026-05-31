package forge.engine;

import forge.data.market.ContractTradeWindow;
import forge.event.MarketEventOccurrence;
import forge.feature.SessionRangeFeature;

import java.util.Collection;
import java.util.List;

public interface QueryDerivedDataStore {
    boolean areSessionRangesBuilt(List<ContractTradeWindow> windows);

    List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows);

    void saveSessionRanges(Collection<SessionRangeFeature> sessionRangeFeatures);

    void markSessionRangesBuilt(List<ContractTradeWindow> windows);

    boolean areMarketEventOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName);

    List<MarketEventOccurrence> loadMarketEventOccurrences(List<ContractTradeWindow> windows, String eventName);

    void saveMarketEventOccurrences(Collection<MarketEventOccurrence> marketEvents);

    void markMarketEventOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName);
}
