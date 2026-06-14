package forge.app;

public class ImportProgress {
    private final String contractSymbol;
    private final long processedRecords;
    private final long totalRecords;

    public ImportProgress(String contractSymbol, long processedRecords, long totalRecords) {
        /*
         * Intent: Capture import completion state for GUI progress displays.
         * Precondition: Contract symbol must be nonblank; processed and total counts must be nonnegative; processed cannot exceed total.
         * Returns: A constructed ImportProgress instance.
         * Postcondition: Progress counts are valid and contract symbol is trimmed.
         */
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

    /*
     * Intent: Convert processed/total records into a normalized completion ratio.
     * Precondition: ImportProgress has validated nonnegative counts.
     * Returns: A value from 0.0 through 1.0, treating zero-total work as complete.
     * Postcondition: Object state is unchanged.
     */
    public double getCompletionRatio() {
        if (totalRecords == 0) {
            return 1.0;
        }
        return (double) processedRecords / totalRecords;
    }

    /*
     * Intent: Convert the completion ratio into a rounded whole-number percentage.
     * Precondition: ImportProgress has validated nonnegative counts.
     * Returns: Integer percent from 0 through 100.
     * Postcondition: Object state is unchanged.
     */
    public int getCompletionPercent() {
        return (int) Math.round(getCompletionRatio() * 100.0);
    }
}
