package forge.data;

import forge.data.InstrumentDataCatalog.AvailableDateRange;
import forge.data.InstrumentDataCatalog.AvailableInstrumentData;

import java.time.LocalDate;
import java.util.List;

public class FacadeForgeData {
    private static final FacadeForgeData THE_INSTANCE = new FacadeForgeData();

    private final InstrumentDataCatalog instrumentDataCatalog;
    private final ForgeDataAccess access = new ForgeDataAccess();

    public static FacadeForgeData getTheInstance() {
        return THE_INSTANCE;
    }

    public FacadeForgeData() {
        this(new InstrumentDataCatalog());
    }

    public FacadeForgeData(InstrumentDataCatalog instrumentDataCatalog) {
        this.instrumentDataCatalog = instrumentDataCatalog;
    }

    public ForgeDataAccess forgeDataAccess() {
        return access;
    }

    public class ForgeDataAccess {
        public List<AvailableInstrumentData> getAvailableInstruments() {
            return instrumentDataCatalog.getAvailableInstruments();
        }

        public AvailableDateRange getSharedDateRange(List<String> symbols) {
            return instrumentDataCatalog.getSharedDateRange(symbols);
        }

        public void validateDateRange(List<String> symbols, LocalDate startDate, LocalDate endDate) {
            instrumentDataCatalog.validateDateRange(symbols, startDate, endDate);
        }
    }
}
