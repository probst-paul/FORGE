package forge.data.importing;

import forge.data.contract.FuturesContractCode;

import java.time.Instant;

public class DataImportPlan {
    private final String contractSymbol;
    private final String instrumentSymbol;
    private final String contractCode;
    private final String tableName;
    private final boolean existingContractTable;
    private final long existingRows;
    private final Instant existingFirstTradeDateTime;
    private final Instant existingLastTradeDateTime;
    private final Instant fileFirstTradeDateTime;
    private final Instant fileLastTradeDateTime;
    private final long overlappingRows;
    private final String currentSourceFileName;
    private final String currentImportStatus;

    public DataImportPlan(
            String contractSymbol,
            String tableName,
            boolean existingContractTable,
            long existingRows,
            String currentSourceFileName,
            String currentImportStatus
    ) {
        this(
                contractSymbol,
                null,
                tableName,
                existingContractTable,
                existingRows,
                null,
                null,
                null,
                null,
                0,
                currentSourceFileName,
                currentImportStatus
        );
    }

    public DataImportPlan(
            String contractSymbol,
            FuturesContractCode contractCode,
            String tableName,
            boolean existingContractTable,
            long existingRows,
            Instant existingFirstTradeDateTime,
            Instant existingLastTradeDateTime,
            Instant fileFirstTradeDateTime,
            Instant fileLastTradeDateTime,
            long overlappingRows,
            String currentSourceFileName,
            String currentImportStatus
    ) {
        this.contractSymbol = requireText(contractSymbol, "contractSymbol is required");
        this.instrumentSymbol = contractCode == null ? instrumentFromContractSymbol(this.contractSymbol) : contractCode.getInstrumentSymbol();
        this.contractCode = contractCode == null ? contractFromContractSymbol(this.contractSymbol) : contractCode.getMonthCode() + String.format("%02d", contractCode.getYear() % 100);
        this.tableName = requireText(tableName, "tableName is required");
        if (existingRows < 0) {
            throw new IllegalArgumentException("existingRows cannot be negative");
        }
        if (overlappingRows < 0) {
            throw new IllegalArgumentException("overlappingRows cannot be negative");
        }
        this.existingContractTable = existingContractTable;
        this.existingRows = existingRows;
        this.existingFirstTradeDateTime = existingFirstTradeDateTime;
        this.existingLastTradeDateTime = existingLastTradeDateTime;
        this.fileFirstTradeDateTime = fileFirstTradeDateTime;
        this.fileLastTradeDateTime = fileLastTradeDateTime;
        this.overlappingRows = overlappingRows;
        this.currentSourceFileName = currentSourceFileName;
        this.currentImportStatus = currentImportStatus;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public String getTableName() {
        return tableName;
    }

    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }

    public String getContractCode() {
        return contractCode;
    }

    public boolean hasExistingContractTable() {
        return existingContractTable;
    }

    public boolean hasExistingRows() {
        return existingRows > 0;
    }

    public long getExistingRows() {
        return existingRows;
    }

    public Instant getExistingFirstTradeDateTime() {
        return existingFirstTradeDateTime;
    }

    public Instant getExistingLastTradeDateTime() {
        return existingLastTradeDateTime;
    }

    public Instant getFileFirstTradeDateTime() {
        return fileFirstTradeDateTime;
    }

    public Instant getFileLastTradeDateTime() {
        return fileLastTradeDateTime;
    }

    public long getOverlappingRows() {
        return overlappingRows;
    }

    public String getCurrentSourceFileName() {
        return currentSourceFileName;
    }

    public String getCurrentImportStatus() {
        return currentImportStatus;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String instrumentFromContractSymbol(String contractSymbol) {
        return contractSymbol.replaceFirst("[FGHJKMNQUVXZ][0-9]{1,2}$", "");
    }

    private static String contractFromContractSymbol(String contractSymbol) {
        return contractSymbol.substring(instrumentFromContractSymbol(contractSymbol).length());
    }
}
