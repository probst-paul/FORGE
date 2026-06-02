package forge.gui.report;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class SavedReport<T extends Serializable> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String reportType;
    private final T report;
    private final LocalDateTime savedAt;

    public SavedReport(String reportType, T report) {
        this(reportType, report, LocalDateTime.now());
    }

    SavedReport(String reportType, T report, LocalDateTime savedAt) {
        if (reportType == null || reportType.trim().isEmpty()) {
            throw new IllegalArgumentException("reportType is required");
        }
        if (report == null) {
            throw new IllegalArgumentException("report is required");
        }
        if (savedAt == null) {
            throw new IllegalArgumentException("savedAt is required");
        }
        this.reportType = reportType.trim();
        this.report = report;
        this.savedAt = savedAt;
    }

    public String getReportType() {
        return reportType;
    }

    public T getReport() {
        return report;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }
}
