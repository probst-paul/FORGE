package forge.gui.viewmodel;

import forge.data.market.ContractTradeWindow;
import forge.util.ImmutableLists;

import java.util.Collections;
import java.util.List;

public class DerivedDataViewModel extends GuiWorkflowViewModel {
    private List<ContractTradeWindow> contractWindows = Collections.emptyList();
    private boolean buildSessionRanges = true;
    private boolean buildFirstHourBreachEvents = true;
    private boolean rebuildExisting;
    private long ticksRead;
    private long sessionRangesBuilt;
    private long marketConditionOccurrencesBuilt;

    public List<ContractTradeWindow> getContractWindows() {
        return contractWindows;
    }

    public void setContractWindows(List<ContractTradeWindow> contractWindows) {
        this.contractWindows = ImmutableLists.copyOfRequired(contractWindows, "contractWindows");
    }

    public boolean shouldBuildSessionRanges() {
        return buildSessionRanges;
    }

    public void setBuildSessionRanges(boolean buildSessionRanges) {
        this.buildSessionRanges = buildSessionRanges;
    }

    public boolean shouldBuildFirstHourBreachEvents() {
        return buildFirstHourBreachEvents;
    }

    public void setBuildFirstHourBreachEvents(boolean buildFirstHourBreachEvents) {
        this.buildFirstHourBreachEvents = buildFirstHourBreachEvents;
    }

    public boolean isRebuildExisting() {
        return rebuildExisting;
    }

    public void setRebuildExisting(boolean rebuildExisting) {
        this.rebuildExisting = rebuildExisting;
    }

    public long getTicksRead() {
        return ticksRead;
    }

    public void setTicksRead(long ticksRead) {
        this.ticksRead = requireNonNegative(ticksRead, "ticksRead");
    }

    public long getSessionRangesBuilt() {
        return sessionRangesBuilt;
    }

    public void setSessionRangesBuilt(long sessionRangesBuilt) {
        this.sessionRangesBuilt = requireNonNegative(sessionRangesBuilt, "sessionRangesBuilt");
    }

    public long getMarketEventOccurrencesBuilt() {
        return marketConditionOccurrencesBuilt;
    }

    public void setMarketEventOccurrencesBuilt(long marketConditionOccurrencesBuilt) {
        this.marketConditionOccurrencesBuilt = requireNonNegative(
                marketConditionOccurrencesBuilt,
                "marketConditionOccurrencesBuilt"
        );
    }
}
