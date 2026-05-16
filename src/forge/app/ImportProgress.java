package forge.app;

public class ImportProgress {
    private final String contractSymbol;
    private final long processedRecords;
    private final long totalRecords;

    public ImportProgress(String contractSymbol, long processedRecords, long totalRecords) {
        if (contractSymbol == null || contractSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("contractSymbol is required");
        }
        if (processedRecords < 0) {
            throw new IllegalArgumentException("processedRecords cannot be negative");
        }
        if (totalRecords < 0) {
            throw new IllegalArgumentException("totalRecords cannot be negative");
        }
        if (processedRecords > totalRecords) {
            throw new IllegalArgumentException("processedRecords cannot exceed totalRecords");
        }
        this.contractSymbol = contractSymbol.trim();
        this.processedRecords = processedRecords;
        this.totalRecords = totalRecords;
    }

    public String getContractSymbol() {
        return contractSymbol;
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public double getCompletionRatio() {
        if (totalRecords == 0) {
            return 1.0;
        }
        return (double) processedRecords / totalRecords;
    }

    public int getCompletionPercent() {
        return (int) Math.round(getCompletionRatio() * 100.0);
    }
}
