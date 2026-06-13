package forge.data.importing;

import forge.app.ImportProgress;
import forge.app.ImportProgressListener;
import forge.data.contract.ContractNameResolver;
import forge.data.contract.FuturesContractCode;
import forge.data.postgres.PostgresTradeRepository;
import forge.data.rollover.ContractRolloverCalendar;
import forge.data.rollover.ContractRolloverWindow;
import forge.model.FuturesInstrumentSpec;
import forge.model.FuturesInstrumentSpecProvider;
import forge.model.StaticFuturesInstrumentSpecProvider;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ScidDataImportService {
    private static final int IMPORT_BATCH_SIZE = 100_000;

    private final ContractNameResolver contractNameResolver;
    private final FuturesInstrumentSpecProvider futuresInstrumentSpecProvider;
    private final ScidTradeReader scidTradeReader;
    private final PostgresTradeRepository tradeRepository;
    private final ContractRolloverCalendar contractRolloverCalendar;

    public ScidDataImportService(ContractNameResolver contractNameResolver, PostgresTradeRepository tradeRepository) {
        /*
         * Intent: Create the SCID import service with default instrument metadata, reader, and rollover calendar.
         * Precondition: Contract resolver and trade repository must be available.
         * Returns: A constructed ScidDataImportService instance.
         * Postcondition: Service can plan and import supported SCID files into PostgreSQL.
         */
        this(
                contractNameResolver,
                new StaticFuturesInstrumentSpecProvider(),
                new ScidTradeReader(),
                tradeRepository,
                new ContractRolloverCalendar()
        );
    }

    public ScidDataImportService(
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider,
            ScidTradeReader scidTradeReader,
            PostgresTradeRepository tradeRepository,
            ContractRolloverCalendar contractRolloverCalendar
    ) {
        /*
         * Intent: Create the SCID import service with explicit parsing, validation, repository, and rollover dependencies.
         * Precondition: All dependencies must be non-null.
         * Returns: A constructed ScidDataImportService instance.
         * Postcondition: Import behavior uses the supplied collaborators.
         */
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        if (futuresInstrumentSpecProvider == null) {
            throw new IllegalArgumentException("futuresInstrumentSpecProvider is required");
        }
        if (scidTradeReader == null) {
            throw new IllegalArgumentException("scidTradeReader is required");
        }
        if (tradeRepository == null) {
            throw new IllegalArgumentException("tradeRepository is required");
        }
        if (contractRolloverCalendar == null) {
            throw new IllegalArgumentException("contractRolloverCalendar is required");
        }
        this.contractNameResolver = contractNameResolver;
        this.futuresInstrumentSpecProvider = futuresInstrumentSpecProvider;
        this.scidTradeReader = scidTradeReader;
        this.tradeRepository = tradeRepository;
        this.contractRolloverCalendar = contractRolloverCalendar;
    }

    public DataImportPlan planImport(String scidFilePath) {
        /*
         * Intent: Inspect whether a SCID file's contract already has stored data before importing.
         * Precondition: SCID path must identify a supported contract file.
         * Returns: DataImportPlan describing the existing contract table/import state.
         * Postcondition: Database and checkpoint table may be created, but no trade rows are imported.
         */
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        FuturesContractCode contractCode = contractNameResolver.resolveContractCode(contractSymbol);
        FuturesInstrumentSpec instrumentSpec = validateSupportedContract(contractSymbol);
        String tableName = contractSymbol;
        Path path = Path.of(scidFilePath);
        Optional<ContractRolloverWindow> activeWindow = contractRolloverCalendar.findActiveWindow(contractSymbol);
        ImportFileBounds fileBounds = scanImportFileBounds(path, instrumentSpec.getTickSize(), activeWindow);
        tradeRepository.ensureDatabaseExists();
        return tradeRepository.planImport(
                contractSymbol,
                contractCode,
                tableName,
                fileBounds.getFirstTradeDateTime(),
                fileBounds.getLastTradeDateTime()
        );
    }

    public PostgresTradeRepository getTradeRepository() {
        return tradeRepository;
    }

    public DataImportResult importScidFile(
            String scidFilePath,
            boolean rebuildExistingContract,
            ImportProgressListener progressListener
    ) {
        return importScidFile(
                scidFilePath,
                rebuildExistingContract ? DataImportMode.OVERWRITE_OVERLAP : DataImportMode.FILL_MISSING,
                progressListener
        );
    }

    public DataImportResult importScidFile(
            String scidFilePath,
            DataImportMode importMode,
            ImportProgressListener progressListener
    ) {
        /*
         * Intent: Import a SCID file into the authoritative contract table with checkpointed batch commits.
         * Precondition: SCID file must be readable, supported, and confirmed for overwrite when requested.
         * Returns: DataImportResult containing row counts and elapsed import time.
         * Postcondition: Front-month rows are stored, checkpoint metadata is updated, and progress is reported.
         */
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        FuturesInstrumentSpec instrumentSpec = validateSupportedContract(contractSymbol);
        String tableName = contractSymbol;
        Path path = Path.of(scidFilePath);
        String sourceFileName = path.getFileName().toString();
        ImportProgressListener listener = progressListener == null ? ImportProgressListener.NO_OP : progressListener;
        long totalRecords = totalRecordCount(path);
        Optional<ContractRolloverWindow> activeWindow = contractRolloverCalendar.findActiveWindow(contractSymbol);
        ImportFileBounds fileBounds = scanImportFileBounds(path, instrumentSpec.getTickSize(), activeWindow);
        DataImportMode normalizedImportMode = importMode == null ? DataImportMode.FILL_MISSING : importMode;
        long importStartNanos = System.nanoTime();

        tradeRepository.ensureDatabaseExists();
        tradeRepository.ensureContractTradesTableExists(tableName);
        tradeRepository.ensureImportCheckpointTableExists();
        tradeRepository.ensureContractRecordUniqueIndex(tableName);
        long overlappingRowsRemoved = normalizedImportMode == DataImportMode.OVERWRITE_OVERLAP
                ? tradeRepository.deleteRowsBetween(
                tableName,
                fileBounds.getFirstTradeDateTime(),
                fileBounds.getLastTradeDateTime()
        )
                : 0;
        ImportCheckpoint checkpoint = tradeRepository.prepareImportCheckpoint(
                tableName,
                sourceFileName,
                fileSize(path),
                lastModifiedMillis(path),
                normalizedImportMode == DataImportMode.OVERWRITE_OVERLAP
        );
        validateCheckpointWithinFile(checkpoint, totalRecords);
        AtomicInteger importedRows = new AtomicInteger();
        AtomicLong duplicateRowsSkipped = new AtomicLong();
        AtomicLong nullSideRowsImported = new AtomicLong();
        AtomicLong skippedOutsideFrontMonthRows = new AtomicLong();
        listener.onProgress(new ImportProgress(
                contractSymbol,
                checkpoint.getNextRecordIndex() - 1,
                totalRecords
        ));
        scidTradeReader.readTrades(
                path,
                checkpoint.getNextRecordIndex(),
                IMPORT_BATCH_SIZE,
                instrumentSpec.getTickSize(),
                trades -> {
                    long nextRecordIndex = trades.get(trades.size() - 1).getScidRecordIndex() + 1;
                    List<TradeRow> frontMonthTrades = filterFrontMonthTrades(trades, activeWindow);
                    skippedOutsideFrontMonthRows.addAndGet(trades.size() - frontMonthTrades.size());
                    nullSideRowsImported.addAndGet(countNullSideRows(frontMonthTrades));
                    if (frontMonthTrades.isEmpty()) {
                        tradeRepository.advanceImportCheckpoint(tableName, sourceFileName, nextRecordIndex);
                    } else {
                        int insertedRows = tradeRepository.insertTradesAndAdvanceCheckpoint(
                                tableName,
                                sourceFileName,
                                frontMonthTrades,
                                nextRecordIndex
                        );
                        importedRows.addAndGet(insertedRows);
                        duplicateRowsSkipped.addAndGet(frontMonthTrades.size() - insertedRows);
                    }
                    listener.onProgress(new ImportProgress(
                            contractSymbol,
                            Math.min(nextRecordIndex - 1, totalRecords),
                            totalRecords
                    ));
                }
        );
        tradeRepository.markImportComplete(tableName, sourceFileName);

        return new DataImportResult(
                tradeRepository.getDatabaseName(),
                tableName,
                contractSymbol,
                normalizedImportMode,
                importedRows.get(),
                duplicateRowsSkipped.get(),
                overlappingRowsRemoved,
                nullSideRowsImported.get(),
                skippedOutsideFrontMonthRows.get(),
                Duration.ofNanos(System.nanoTime() - importStartNanos)
        );
    }

    private ImportFileBounds scanImportFileBounds(
            Path path,
            double tickSize,
            Optional<ContractRolloverWindow> activeWindow
    ) {
        /*
         * Intent: Determine the selected file's importable timestamp range before applying import mode behavior.
         * Precondition: Path must identify a valid SCID file and tick size must be valid.
         * Returns: First/last timestamp from rows that pass rollover filtering.
         * Postcondition: Database state is unchanged.
         */
        ImportFileBounds bounds = new ImportFileBounds();
        scidTradeReader.readTrades(
                path,
                1,
                IMPORT_BATCH_SIZE,
                tickSize,
                trades -> {
                    List<TradeRow> frontMonthTrades = filterFrontMonthTrades(trades, activeWindow);
                    for (TradeRow trade : frontMonthTrades) {
                        bounds.include(trade);
                    }
                }
        );
        return bounds;
    }

    private List<TradeRow> filterFrontMonthTrades(
            List<TradeRow> trades,
            Optional<ContractRolloverWindow> activeWindow
    ) {
        /*
         * Intent: Keep only trades inside a contract's active front-month rollover window.
         * Precondition: Trades must be non-null and active window may be absent for unsupported rollover families.
         * Returns: Original list when no window exists, otherwise filtered front-month trades.
         * Postcondition: Source trade list is not modified.
         */
        if (activeWindow.isEmpty()) {
            return trades;
        }
        ContractRolloverWindow window = activeWindow.get();
        return trades.stream()
                .filter(trade -> isWithinActiveWindow(trade, window))
                .collect(Collectors.toList());
    }

    private boolean isWithinActiveWindow(TradeRow trade, ContractRolloverWindow activeWindow) {
        LocalDate tradeDate = trade.getTradeDateTime().atZone(ZoneOffset.UTC).toLocalDate();
        return !tradeDate.isBefore(activeWindow.getActiveStartDate())
                && !tradeDate.isAfter(activeWindow.getActiveEndDate());
    }

    private long countNullSideRows(List<TradeRow> trades) {
        return trades.stream()
                .filter(trade -> trade.getSide() == null)
                .count();
    }

    private long totalRecordCount(Path path) {
        /*
         * Intent: Calculate expected SCID record count from fixed file layout.
         * Precondition: Path must point to a readable SCID file with a 56-byte header and 40-byte records.
         * Returns: Number of complete records in the file.
         * Postcondition: File contents are not read beyond metadata size lookup.
         */
        long dataBytes = fileSize(path) - 56;
        if (dataBytes < 0 || dataBytes % 40 != 0) {
            throw new IllegalArgumentException("SCID file size does not match the expected header and record sizes");
        }
        return dataBytes / 40;
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read SCID file size: " + path, exception);
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read SCID file modified time: " + path, exception);
        }
    }

    private void validateCheckpointWithinFile(ImportCheckpoint checkpoint, long totalRecords) {
        /*
         * Intent: Guard against resuming an import from metadata that points beyond the selected file.
         * Precondition: Checkpoint and total record count must describe the same intended import source.
         * Returns: Nothing.
         * Postcondition: Invalid checkpoint state stops import with a recovery message.
         */
        if (checkpoint.getNextRecordIndex() > totalRecords + 1) {
            throw new IllegalStateException(
                    "Import checkpoint for " + checkpoint.getTableName() +
                            " points past the end of the selected SCID file. " +
                            "The file may have changed or the previous import metadata is invalid. " +
                            "Wipe and rebuild the contract table with a known-good SCID file."
            );
        }
    }

    private FuturesInstrumentSpec validateSupportedContract(String contractSymbol) {
        /*
         * Intent: Ensure the contract root and expiration month are supported before import touches trade data.
         * Precondition: Contract symbol must be parseable by ContractNameResolver.
         * Returns: Instrument spec for the contract root.
         * Postcondition: Unsupported roots/months fail fast with file-quality guidance.
         */
        FuturesContractCode contractCode = contractNameResolver.resolveContractCode(contractSymbol);
        FuturesInstrumentSpec instrumentSpec = futuresInstrumentSpecProvider.getBySymbol(contractCode.getInstrumentSymbol());
        if (!instrumentSpec.supportsMonthCode(contractCode.getMonthCode())) {
            throw new IllegalArgumentException(
                    "Unsupported contract month " + contractCode.getMonthCode() +
                            " for " + contractCode.getInstrumentSymbol() +
                            ". Supported months are " + String.join(", ", instrumentSpec.getSupportedMonthCodes()) +
                            ". The SCID file may be corrupted or named for a contract that does not exist; use a known-good data file."
            );
        }
        return instrumentSpec;
    }

    private static class ImportFileBounds {
        private java.time.Instant firstTradeDateTime;
        private java.time.Instant lastTradeDateTime;

        private void include(TradeRow trade) {
            java.time.Instant tradeDateTime = trade.getTradeDateTime();
            if (firstTradeDateTime == null || tradeDateTime.isBefore(firstTradeDateTime)) {
                firstTradeDateTime = tradeDateTime;
            }
            if (lastTradeDateTime == null || tradeDateTime.isAfter(lastTradeDateTime)) {
                lastTradeDateTime = tradeDateTime;
            }
        }

        private java.time.Instant getFirstTradeDateTime() {
            return firstTradeDateTime;
        }

        private java.time.Instant getLastTradeDateTime() {
            return lastTradeDateTime;
        }
    }
}
