package forge.gui.report;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class GuiReportStore {
    private static final Path DEFAULT_REPORT_DIRECTORY = Path.of(
            System.getProperty("user.dir"),
            "runtime",
            "reports"
    );

    private final Path reportDirectory;

    public GuiReportStore() {
        this(DEFAULT_REPORT_DIRECTORY);
    }

    public GuiReportStore(Path reportDirectory) {
        if (reportDirectory == null) {
            throw new IllegalArgumentException("reportDirectory is required");
        }
        this.reportDirectory = reportDirectory;
    }

    public Path getReportDirectory() {
        return reportDirectory;
    }

    public Path ensureReportDirectory() {
        try {
            Files.createDirectories(reportDirectory);
            return reportDirectory;
        } catch (IOException exception) {
            throw new RuntimeException("Could not prepare report directory.", exception);
        }
    }

    public <T extends Serializable> void save(Path reportPath, SavedReport<T> report) {
        /*
         * Intent: Persist a user-selected report snapshot with Java object serialization.
         * Precondition: reportPath and report must be non-null.
         * Returns: Nothing.
         * Postcondition: Parent directories exist and the selected .dat file is written.
         */
        if (reportPath == null) {
            throw new IllegalArgumentException("reportPath is required");
        }
        if (report == null) {
            throw new IllegalArgumentException("report is required");
        }
        try {
            Path parent = reportPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(reportPath))) {
                output.writeObject(report);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Could not save report.", exception);
        }
    }

    public <T extends Serializable> SavedReport<T> load(
            Path reportPath,
            Class<T> reportClass,
            String expectedReportType
    ) {
        /*
         * Intent: Load and validate a saved report snapshot from a .dat file.
         * Precondition: reportPath, reportClass, and expectedReportType must identify the expected report.
         * Returns: SavedReport containing the expected report type.
         * Postcondition: Invalid or incompatible files are rejected with a runtime exception.
         */
        if (reportPath == null) {
            throw new IllegalArgumentException("reportPath is required");
        }
        if (reportClass == null) {
            throw new IllegalArgumentException("reportClass is required");
        }
        if (expectedReportType == null || expectedReportType.trim().isEmpty()) {
            throw new IllegalArgumentException("expectedReportType is required");
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(reportPath))) {
            Object loaded = input.readObject();
            if (!(loaded instanceof SavedReport<?> savedReport)) {
                throw new IllegalArgumentException("File does not contain a saved FORGE report.");
            }
            if (!expectedReportType.equals(savedReport.getReportType())) {
                throw new IllegalArgumentException("Saved report type is not " + expectedReportType + ".");
            }
            Object report = savedReport.getReport();
            if (!reportClass.isInstance(report)) {
                throw new IllegalArgumentException("Saved report payload is not compatible.");
            }
            return new SavedReport<>(savedReport.getReportType(), reportClass.cast(report), savedReport.getSavedAt());
        } catch (IOException | ClassNotFoundException exception) {
            throw new RuntimeException("Could not load report.", exception);
        }
    }
}
