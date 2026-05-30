package forge.data.catalog;

import forge.data.contract.ContractNameResolver;
import forge.data.postgres.PostgresDatabaseSettings;
import forge.data.postgres.PostgresTradeRepository;
import forge.data.rollover.ContractRolloverCalendar;
import forge.data.rollover.ContractRolloverWindow;
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
    private final ContractRolloverCalendar contractRolloverCalendar;

    public InstrumentDataCatalog() {
        /*
         * Intent: Create the instrument catalog using environment-based PostgreSQL settings.
         * Precondition: PostgreSQL settings may be resolved from environment variables or defaults.
         * Returns: A constructed InstrumentDataCatalog instance.
         * Postcondition: Catalog reads availability from imported contract metadata.
         */
        this(new PostgresTradeRepository(PostgresDatabaseSettings.fromEnvironment()));
    }

    public InstrumentDataCatalog(PostgresTradeRepository tradeRepository) {
        /*
         * Intent: Create the instrument catalog from a PostgreSQL trade repository.
         * Precondition: Repository must expose imported contract summaries.
         * Returns: A constructed InstrumentDataCatalog instance.
         * Postcondition: Catalog groups imported contracts by supported futures instrument roots.
         */
        this(
                tradeRepository::listImportedContractData,
                new ContractNameResolver(),
                new StaticFuturesInstrumentSpecProvider(),
                new ContractRolloverCalendar()
        );
    }

    InstrumentDataCatalog(
            Supplier<List<ContractDataSummary>> contractDataSource,
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider,
            ContractRolloverCalendar contractRolloverCalendar
    ) {
        /*
         * Intent: Create the instrument catalog with explicit data source, contract parsing, instrument specs, and rollover rules.
         * Precondition: All dependencies must be non-null.
         * Returns: A constructed InstrumentDataCatalog instance.
         * Postcondition: Availability calculations use the supplied source and calendar.
         */
        if (contractDataSource == null) {
            throw new IllegalArgumentException("contractDataSource is required");
        }
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        if (futuresInstrumentSpecProvider == null) {
            throw new IllegalArgumentException("futuresInstrumentSpecProvider is required");
        }
        if (contractRolloverCalendar == null) {
            throw new IllegalArgumentException("contractRolloverCalendar is required");
        }
        this.contractDataSource = contractDataSource;
        this.contractNameResolver = contractNameResolver;
        this.futuresInstrumentSpecProvider = futuresInstrumentSpecProvider;
        this.contractRolloverCalendar = contractRolloverCalendar;
    }

    public List<AvailableInstrumentData> getAvailableInstruments() {
        /*
         * Intent: List root-level instruments that have imported front-month-valid contract data.
         * Precondition: Contract metadata source must be available.
         * Returns: Immutable list of available instruments with combined date coverage.
         * Postcondition: Catalog state is unchanged.
         */
        return Collections.unmodifiableList(new ArrayList<>(loadAvailableData().values()));
    }

    public List<AvailableContractData> getAvailableContracts() {
        /*
         * Intent: List individual imported contract windows clipped to their active front-month windows.
         * Precondition: Contract metadata source and rollover calendar must be available.
         * Returns: Immutable list of available contracts.
         * Postcondition: Contracts outside active rollover windows are omitted.
         */
        List<AvailableContractData> availableContracts = new ArrayList<>();
        for (ContractDataSummary summary : contractDataSource.get()) {
            String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(summary.getContractSymbol());
            if (!futuresInstrumentSpecProvider.supports(instrumentSymbol)) {
                continue;
            }
            ContractDataSummary activeSummary = clipToActiveWindow(summary);
            if (activeSummary != null) {
                availableContracts.add(new AvailableContractData(
                        activeSummary.getContractSymbol(),
                        instrumentSymbol,
                        activeSummary.getStartDate(),
                        activeSummary.getEndDate()
                ));
            }
        }
        return Collections.unmodifiableList(availableContracts);
    }

    public AvailableDateRange getSharedDateRange(List<String> symbols) {
        /*
         * Intent: Find overlapping availability across selected root-level instruments.
         * Precondition: Symbols list must contain at least one available instrument symbol.
         * Returns: Shared date range where all selected instruments have front-month-valid data.
         * Postcondition: Catalog state is unchanged.
         */
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
        /*
         * Intent: Validate a user-selected date range against available instrument data.
         * Precondition: Symbols and dates must be supplied.
         * Returns: Nothing.
         * Postcondition: Invalid ranges fail with a user-facing exception.
         */
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
        /*
         * Intent: Aggregate contract metadata into root-level instrument availability.
         * Precondition: Contract metadata source must return parseable contract summaries.
         * Returns: Map keyed by instrument root symbol.
         * Postcondition: Availability is clipped to active rollover windows before aggregation.
         */
        Map<String, InstrumentDateBounds> boundsByInstrument = new LinkedHashMap<>();
        for (ContractDataSummary summary : contractDataSource.get()) {
            String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(summary.getContractSymbol());
            if (!futuresInstrumentSpecProvider.supports(instrumentSymbol)) {
                continue;
            }
            ContractDataSummary activeSummary = clipToActiveWindow(summary);
            if (activeSummary == null) {
                continue;
            }
            InstrumentDateBounds bounds = boundsByInstrument.computeIfAbsent(instrumentSymbol, key -> new InstrumentDateBounds());
            bounds.include(activeSummary.getStartDate(), activeSummary.getEndDate());
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

    private ContractDataSummary clipToActiveWindow(ContractDataSummary summary) {
        /*
         * Intent: Apply a rollover window when one exists for the contract's instrument family.
         * Precondition: Summary must describe an imported contract's raw available date range.
         * Returns: Clipped summary, original summary, or null when no valid front-month overlap exists.
         * Postcondition: Source summary is unchanged.
         */
        return contractRolloverCalendar.findActiveWindow(summary.getContractSymbol())
                .map(activeWindow -> clipToActiveWindow(summary, activeWindow))
                .orElse(summary);
    }

    private ContractDataSummary clipToActiveWindow(ContractDataSummary summary, ContractRolloverWindow activeWindow) {
        LocalDate clippedStartDate = max(summary.getStartDate(), activeWindow.getActiveStartDate());
        LocalDate clippedEndDate = min(summary.getEndDate(), activeWindow.getActiveEndDate());
        if (clippedEndDate.isBefore(clippedStartDate)) {
            return null;
        }
        return new ContractDataSummary(summary.getContractSymbol(), clippedStartDate, clippedEndDate);
    }

    private LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private AvailableInstrumentData getInstrumentData(Map<String, AvailableInstrumentData> availableData, String symbol) {
        /*
         * Intent: Resolve one selected symbol into catalog availability data.
         * Precondition: Symbol must be nonblank and present in the loaded availability map.
         * Returns: AvailableInstrumentData for the symbol.
         * Postcondition: Availability map is unchanged.
         */
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

    public static class AvailableContractData {
        private final String contractSymbol;
        private final String instrumentSymbol;
        private final LocalDate startDate;
        private final LocalDate endDate;

        public AvailableContractData(String contractSymbol, String instrumentSymbol, LocalDate startDate, LocalDate endDate) {
            /*
             * Intent: Represent one contract's available front-month-valid date window.
             * Precondition: Symbols and dates must be valid and end date must not precede start date.
             * Returns: A constructed AvailableContractData instance.
             * Postcondition: Symbols are normalized to uppercase.
             */
            if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
                throw new IllegalArgumentException("contractSymbol is required");
            }
            if (instrumentSymbol == null || instrumentSymbol.trim().isEmpty()) {
                throw new IllegalArgumentException("instrumentSymbol is required");
            }
            if (startDate == null) {
                throw new IllegalArgumentException("startDate is required");
            }
            if (endDate == null) {
                throw new IllegalArgumentException("endDate is required");
            }
            if (endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("endDate cannot be before startDate");
            }
            this.contractSymbol = contractSymbol.trim().toUpperCase();
            this.instrumentSymbol = instrumentSymbol.trim().toUpperCase();
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getContractSymbol() {
            return contractSymbol;
        }

        public String getInstrumentSymbol() {
            return instrumentSymbol;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        @Override
        public String toString() {
            return contractSymbol + ": " + startDate + " to " + endDate;
        }
    }

    private static class InstrumentDateBounds {
        private LocalDate startDate;
        private LocalDate endDate;

        public void include(LocalDate candidateStartDate, LocalDate candidateEndDate) {
            /*
             * Intent: Expand instrument-level availability to include one contract window.
             * Precondition: Candidate dates should be non-null and ordered.
             * Returns: Nothing.
             * Postcondition: Stored bounds cover both previous bounds and the candidate window.
             */
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
