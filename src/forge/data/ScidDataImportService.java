package forge.data;

import forge.app.ImportProgress;
import forge.app.ImportProgressListener;
import forge.model.FuturesInstrumentSpecProvider;
import forge.model.StaticFuturesInstrumentSpecProvider;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ScidDataImportService {
    private static final int IMPORT_BATCH_SIZE = 10_000;

    private final ContractNameResolver contractNameResolver;
    private final FuturesInstrumentSpecProvider futuresInstrumentSpecProvider;
    private final ScidTradeReader scidTradeReader;
    private final PostgresTradeRepository tradeRepository;

    public ScidDataImportService(ContractNameResolver contractNameResolver, PostgresTradeRepository tradeRepository) {
        this(contractNameResolver, new StaticFuturesInstrumentSpecProvider(), new ScidTradeReader(), tradeRepository);
    }

    public ScidDataImportService(
            ContractNameResolver contractNameResolver,
            FuturesInstrumentSpecProvider futuresInstrumentSpecProvider,
            ScidTradeReader scidTradeReader,
            PostgresTradeRepository tradeRepository
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
        this.contractNameResolver = contractNameResolver;
        this.futuresInstrumentSpecProvider = futuresInstrumentSpecProvider;
        this.scidTradeReader = scidTradeReader;
        this.tradeRepository = tradeRepository;
    }

    public DataImportPlan planImport(String scidFilePath) {
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        validateSupportedInstrument(contractSymbol);
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
        validateSupportedInstrument(contractSymbol);
        String tableName = contractSymbol;
        Path path = Path.of(scidFilePath);
        String sourceFileName = path.getFileName().toString();
        ImportProgressListener listener = progressListener == null ? ImportProgressListener.NO_OP : progressListener;
        long totalRecords = totalRecordCount(path);

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
        tradeRepository.ensureContractRecordUniqueIndex(tableName);
        AtomicInteger importedRows = new AtomicInteger();
        listener.onProgress(new ImportProgress(
                contractSymbol,
                checkpoint.getNextRecordIndex() - 1,
                totalRecords
        ));
        scidTradeReader.readTrades(
                path,
                checkpoint.getNextRecordIndex(),
                IMPORT_BATCH_SIZE,
                trades -> {
                    long nextRecordIndex = trades.get(trades.size() - 1).getScidRecordIndex() + 1;
                    importedRows.addAndGet(tradeRepository.insertTradesAndAdvanceCheckpoint(
                            tableName,
                            sourceFileName,
                            trades,
                            nextRecordIndex
                    ));
                    listener.onProgress(new ImportProgress(
                            contractSymbol,
                            Math.min(nextRecordIndex - 1, totalRecords),
                            totalRecords
                    ));
                }
        );
        tradeRepository.markImportComplete(tableName, sourceFileName);
        listener.onProgress(new ImportProgress(contractSymbol, totalRecords, totalRecords));

        return new DataImportResult(tradeRepository.getDatabaseName(), tableName, contractSymbol, importedRows.get());
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

    private void validateSupportedInstrument(String contractSymbol) {
        String instrumentSymbol = contractNameResolver.resolveInstrumentSymbol(contractSymbol);
        futuresInstrumentSpecProvider.getBySymbol(instrumentSymbol);
    }
}
