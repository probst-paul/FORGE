package forge.data.build;

import forge.data.market.ContractTradeWindow;
import forge.condition.MarketConditionOccurrence;
import forge.feature.SessionRangeFeature;

import java.util.Collection;
import java.util.List;

public interface DerivedDataBuildStore {
    boolean areSessionRangesBuilt(List<ContractTradeWindow> windows);

    List<SessionRangeFeature> loadSessionRanges(List<ContractTradeWindow> windows);

    void saveSessionRanges(Collection<SessionRangeFeature> sessionRangeFeatures);

    void markSessionRangesBuilt(List<ContractTradeWindow> windows);

    void clearSessionRanges(List<ContractTradeWindow> windows);

    boolean areMarketConditionOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName);

    List<MarketConditionOccurrence> loadMarketConditionOccurrences(List<ContractTradeWindow> windows, String eventName);

    void saveMarketConditionOccurrences(Collection<MarketConditionOccurrence> marketEvents);

    void markMarketConditionOccurrencesBuilt(List<ContractTradeWindow> windows, String eventName);

    void clearMarketConditionOccurrences(List<ContractTradeWindow> windows, String eventName);
}
