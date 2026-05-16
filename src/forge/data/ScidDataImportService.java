package forge.data;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ScidDataImportService {
    private static final int IMPORT_BATCH_SIZE = 10_000;

    private final ContractNameResolver contractNameResolver;
    private final ScidTradeReader scidTradeReader;
    private final PostgresTradeRepository tradeRepository;

    public ScidDataImportService(ContractNameResolver contractNameResolver, PostgresTradeRepository tradeRepository) {
        this(contractNameResolver, new ScidTradeReader(), tradeRepository);
    }

    public ScidDataImportService(
            ContractNameResolver contractNameResolver,
            ScidTradeReader scidTradeReader,
            PostgresTradeRepository tradeRepository
    ) {
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        if (scidTradeReader == null) {
            throw new IllegalArgumentException("scidTradeReader is required");
        }
        if (tradeRepository == null) {
            throw new IllegalArgumentException("tradeRepository is required");
        }
        this.contractNameResolver = contractNameResolver;
        this.scidTradeReader = scidTradeReader;
        this.tradeRepository = tradeRepository;
    }

    public DataImportPlan planImport(String scidFilePath) {
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        String tableName = contractSymbol;
        tradeRepository.ensureDatabaseExists();
        return tradeRepository.planImport(contractSymbol, tableName);
    }

    public DataImportResult importScidFile(String scidFilePath, boolean rebuildExistingContract) {
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        String tableName = contractSymbol;
        Path path = Path.of(scidFilePath);
        String sourceFileName = path.getFileName().toString();

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
                }
        );
        tradeRepository.markImportComplete(tableName, sourceFileName);

        return new DataImportResult(tradeRepository.getDatabaseName(), tableName, contractSymbol, importedRows.get());
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
}
