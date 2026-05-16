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
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        validateSupportedContract(contractSymbol);
        String tableName = contractSymbol;
        tradeRepository.ensureDatabaseExists();
        return tradeRepository.planImport(contractSymbol, tableName);
    }

    public DataImportResult importScidFile(
            String scidFilePath,
            boolean rebuildExistingContract,
            ImportProgressListener progressListener
    ) {
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        FuturesInstrumentSpec instrumentSpec = validateSupportedContract(contractSymbol);
        String tableName = contractSymbol;
        Path path = Path.of(scidFilePath);
        String sourceFileName = path.getFileName().toString();
        ImportProgressListener listener = progressListener == null ? ImportProgressListener.NO_OP : progressListener;
        long totalRecords = totalRecordCount(path);
        Optional<ContractRolloverWindow> activeWindow = contractRolloverCalendar.findActiveWindow(contractSymbol);
        long importStartNanos = System.nanoTime();

        tradeRepository.ensureDatabaseExists();
        tradeRepository.ensureContractTradesTableExists(tableName);
        tradeRepository.ensureImportCheckpointTableExists();
        ImportCheckpoint checkpoint = tradeRepository.prepareImportCheckpoint(
                tableName,
                sourceFileName,
                fileSize(path),
                lastModifiedMillis(path),
                rebuildExistingContract
        );
        validateCheckpointWithinFile(checkpoint, totalRecords);
        tradeRepository.ensureContractRecordUniqueIndex(tableName);
        AtomicInteger importedRows = new AtomicInteger();
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
                        importedRows.addAndGet(tradeRepository.insertTradesAndAdvanceCheckpoint(
                                tableName,
                                sourceFileName,
                                frontMonthTrades,
                                nextRecordIndex
                        ));
                    }
                    listener.onProgress(new ImportProgress(
                            contractSymbol,
                            Math.min(nextRecordIndex - 1, totalRecords),
                            totalRecords
                    ));
                }
        );
        tradeRepository.markImportComplete(tableName, sourceFileName);
        listener.onProgress(new ImportProgress(contractSymbol, totalRecords, totalRecords));

        return new DataImportResult(
                tradeRepository.getDatabaseName(),
                tableName,
                contractSymbol,
                importedRows.get(),
                nullSideRowsImported.get(),
                skippedOutsideFrontMonthRows.get(),
                Duration.ofNanos(System.nanoTime() - importStartNanos)
        );
    }

    private List<TradeRow> filterFrontMonthTrades(
            List<TradeRow> trades,
            Optional<ContractRolloverWindow> activeWindow
    ) {
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
}
