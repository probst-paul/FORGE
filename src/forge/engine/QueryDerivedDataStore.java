package forge.engine;

import forge.data.market.ContractTradeWindow;
import forge.engine.eventstatistics.EventStatisticsDetail;
import forge.engine.eventstatistics.EventStatisticsResult;
import forge.event.MarketEventOccurrence;
import forge.feature.SessionRangeFeature;

import java.util.Collection;
import java.util.Collections;
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

    default List<EventStatisticsDetail> loadEventStatisticsDetails(List<ContractTradeWindow> windows, String eventName) {
        return Collections.emptyList();
    }

    default List<EventStatisticsResult> loadEventStatisticsContractResults(List<ContractTradeWindow> windows, String eventName) {
        return Collections.emptyList();
    }
}
