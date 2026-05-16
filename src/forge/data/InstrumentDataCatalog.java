package forge.data;

import forge.model.FuturesInstrument;
import forge.model.FuturesInstrumentSpec;
import forge.model.FuturesInstrumentSpecProvider;
import forge.model.StaticFuturesInstrumentSpecProvider;
import forge.model.Instrument;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class InstrumentDataCatalog {
    private final Supplier<List<ContractDataSummary>> contractDataSource;
    private final ContractNameResolver contractNameResolver;
    private final FuturesInstrumentSpecProvider futuresInstrumentSpecProvider;

    public InstrumentDataCatalog() {
        this(new PostgresTradeRepository(PostgresDatabaseSettings.fromEnvironment()));
    }

    public InstrumentDataCatalog(PostgresTradeRepository tradeRepository) {
        this(
                tradeRepository::listImportedContractData,
                new ContractNameResolver(),
                new StaticFuturesInstrumentSpecProvider()
        );
    }

    InstrumentDataCatalog(
            Supplier<List<ContractDataSummary>> contractDataSource,
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider
    ) {
        if (contractDataSource == null) {
            throw new IllegalArgumentException("contractDataSource is required");
        }
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        if (futuresInstrumentSpecProvider == null) {
            throw new IllegalArgumentException("futuresInstrumentSpecProvider is required");
        }
        this.contractDataSource = contractDataSource;
        this.contractNameResolver = contractNameResolver;
        this.futuresInstrumentSpecProvider = futuresInstrumentSpecProvider;
    }

    public List<AvailableInstrumentData> getAvailableInstruments() {
        return Collections.unmodifiableList(new ArrayList<>(loadAvailableData().values()));
    }

    public AvailableDateRange getSharedDateRange(List<String> symbols) {
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("at least one instrument must be selected");
        }

        LocalDate sharedStart = null;
        LocalDate sharedEnd = null;
        Map<String, AvailableInstrumentData> availableData = loadAvailableData();
        for (String symbol : symbols) {
            AvailableInstrumentData instrumentData = getInstrumentData(availableData, symbol);
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
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("endDate is required");
        }
        AvailableDateRange availableDateRange = getSharedDateRange(symbols);
        if (startDate.isBefore(availableDateRange.getStartDate()) || endDate.isAfter(availableDateRange.getEndDate())) {
            throw new IllegalArgumentException("date range must be within available instrument data");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("end date cannot be before start date");
        }
    }

    private Map<String, AvailableInstrumentData> loadAvailableData() {
        Map<String, InstrumentDateBounds> boundsByInstrument = new LinkedHashMap<>();
        for (ContractDataSummary summary : contractDataSource.get()) {
            String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(summary.getContractSymbol());
            if (!futuresInstrumentSpecProvider.supports(instrumentSymbol)) {
                continue;
            }
            InstrumentDateBounds bounds = boundsByInstrument.computeIfAbsent(instrumentSymbol, key -> new InstrumentDateBounds());
            bounds.include(summary.getStartDate(), summary.getEndDate());
        }

        Map<String, AvailableInstrumentData> availableData = new LinkedHashMap<>();
        for (Map.Entry<String, InstrumentDateBounds> entry : boundsByInstrument.entrySet()) {
            FuturesInstrumentSpec spec = futuresInstrumentSpecProvider.getBySymbol(entry.getKey());
            InstrumentDateBounds bounds = entry.getValue();
            addInstrumentData(
                    availableData,
                    createFuturesInstrument(spec),
                    bounds.getStartDate(),
                    bounds.getEndDate()
            );
        }
        return availableData;
    }

    private AvailableInstrumentData getInstrumentData(Map<String, AvailableInstrumentData> availableData, String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("symbol is required");
        }
        AvailableInstrumentData instrumentData = availableData.get(symbol.trim().toUpperCase());
        if (instrumentData == null) {
            throw new IllegalArgumentException("instrument is not available: " + symbol);
        }
        return instrumentData;
    }

    private static Instrument createFuturesInstrument(FuturesInstrumentSpec spec) {
        return new FuturesInstrument(
                spec.getSymbolCode(),
                spec.getDisplayName(),
                spec.getTickSize(),
                spec.getTickDollarAmount()
        );
    }

    private static void addInstrumentData(
            Map<String, AvailableInstrumentData> data,
            Instrument instrument,
            LocalDate startDate,
            LocalDate endDate
    ) {
        data.put(instrument.getSymbolCode(), new AvailableInstrumentData(instrument, startDate, endDate));
    }

    public static class AvailableInstrumentData {
        private final Instrument instrument;
        private final LocalDate startDate;
        private final LocalDate endDate;

        public AvailableInstrumentData(Instrument instrument, LocalDate startDate, LocalDate endDate) {
            this.instrument = instrument;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public Instrument getInstrument() {
            return instrument;
        }

        public String getSymbol() {
            return instrument.getSymbolCode();
        }

        public double getFuturesTickSize() {
            return asFuturesInstrument().getTickSize();
        }

        public double getFuturesTickDollarAmount() {
            return asFuturesInstrument().getTickDollarAmount();
        }

        public LocalDate getFuturesExpirationDate() {
            throw new UnsupportedOperationException("Instrument-level catalog entries do not have a single futures expiration date");
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        @Override
        public String toString() {
            return instrument.getSymbolCode() + " (" + startDate + " to " + endDate + ")";
        }

        private FuturesInstrument asFuturesInstrument() {
            if (instrument instanceof FuturesInstrument) {
                return (FuturesInstrument) instrument;
            }
            throw new IllegalStateException("Instrument is not a futures instrument: " + instrument.getSymbolCode());
        }
    }

    private static class InstrumentDateBounds {
        private LocalDate startDate;
        private LocalDate endDate;

        public void include(LocalDate candidateStartDate, LocalDate candidateEndDate) {
            if (startDate == null || candidateStartDate.isBefore(startDate)) {
                startDate = candidateStartDate;
            }
            if (endDate == null || candidateEndDate.isAfter(endDate)) {
                endDate = candidateEndDate;
            }
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
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
