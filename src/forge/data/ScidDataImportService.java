package forge.data;

public class ScidDataImportService {
    private final ContractNameResolver contractNameResolver;
    private final PostgresTradeRepository tradeRepository;

    public ScidDataImportService(ContractNameResolver contractNameResolver, PostgresTradeRepository tradeRepository) {
        if (contractNameResolver == null) {
            throw new IllegalArgumentException("contractNameResolver is required");
        }
        if (tradeRepository == null) {
            throw new IllegalArgumentException("tradeRepository is required");
        }
        this.contractNameResolver = contractNameResolver;
        this.tradeRepository = tradeRepository;
    }

    public DataImportResult importScidFile(String scidFilePath) {
        String contractSymbol = contractNameResolver.resolveFromScidPath(scidFilePath);
        String tableName = contractSymbol;

        tradeRepository.ensureDatabaseExists();
        tradeRepository.ensureContractTradesTableExists(tableName);

        return new DataImportResult(tradeRepository.getDatabaseName(), tableName, contractSymbol, 0);
    }
}
