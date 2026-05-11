package forge.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InstrumentDataCatalog {
    private final Map<String, AvailableInstrumentData> availableData;

    public InstrumentDataCatalog() {
        Map<String, AvailableInstrumentData> data = new LinkedHashMap<>();
        data.put("ES", new AvailableInstrumentData("ES", LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 29)));
        data.put("NQ", new AvailableInstrumentData("NQ", LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 29)));
        data.put("CL", new AvailableInstrumentData("CL", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 15)));
        this.availableData = Collections.unmodifiableMap(data);
    }

    public List<AvailableInstrumentData> getAvailableInstruments() {
        return Collections.unmodifiableList(new ArrayList<>(availableData.values()));
    }

    public AvailableDateRange getSharedDateRange(List<String> symbols) {
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("at least one instrument must be selected");
        }

        LocalDate sharedStart = null;
        LocalDate sharedEnd = null;
        for (String symbol : symbols) {
            AvailableInstrumentData instrumentData = getInstrumentData(symbol);
            if (sharedStart == null || instrumentData.getStartDate().isAfter(sharedStart)) {
                sharedStart = instrumentData.getStartDate();
            }
            if (sharedEnd == null || instrumentData.getEndDate().isBefore(sharedEnd)) {
                sharedEnd = instrumentData.getEndDate();
            }
        }

        if (sharedEnd.isBefore(sharedStart)) {
            throw new IllegalArgumentException("selected instruments do not have an overlapping date range");
        }
        return new AvailableDateRange(sharedStart, sharedEnd);
    }

    public void validateDateRange(List<String> symbols, LocalDate startDate, LocalDate endDate) {
        AvailableDateRange availableDateRange = getSharedDateRange(symbols);
        if (startDate.isBefore(availableDateRange.getStartDate()) || endDate.isAfter(availableDateRange.getEndDate())) {
            throw new IllegalArgumentException("date range must be within available instrument data");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end date cannot be before start date");
        }
    }

    private AvailableInstrumentData getInstrumentData(String symbol) {
        AvailableInstrumentData instrumentData = availableData.get(symbol.toUpperCase());
        if (instrumentData == null) {
            throw new IllegalArgumentException("instrument is not available: " + symbol);
        }
        return instrumentData;
    }

    public static class AvailableInstrumentData {
        private final String symbol;
        private final LocalDate startDate;
        private final LocalDate endDate;

        public AvailableInstrumentData(String symbol, LocalDate startDate, LocalDate endDate) {
            this.symbol = symbol;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getSymbol() {
            return symbol;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        @Override
        public String toString() {
            return symbol + " (" + startDate + " to " + endDate + ")";
        }
    }

    public static class AvailableDateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        public AvailableDateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        @Override
        public String toString() {
            return startDate + " to " + endDate;
        }
    }
}
