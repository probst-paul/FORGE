package forge.data;

import forge.data.InstrumentDataCatalog.AvailableDateRange;
import forge.data.InstrumentDataCatalog.AvailableInstrumentData;

import java.time.LocalDate;
import java.util.List;

public class FacadeData {
    private final InstrumentDataCatalog instrumentDataCatalog;

    public FacadeData() {
        this(new InstrumentDataCatalog());
    }

    public FacadeData(InstrumentDataCatalog instrumentDataCatalog) {
        this.instrumentDataCatalog = instrumentDataCatalog;
    }

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
