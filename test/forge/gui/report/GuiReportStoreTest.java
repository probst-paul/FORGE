package forge.gui.report;

import forge.engine.backtest.BacktestResult;
import forge.engine.eventstatistics.EventStatisticsResult;
import forge.reporting.eventstatistics.EventStatisticsReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiReportStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadEventStatisticsReportDatFile() {
        GuiReportStore store = new GuiReportStore(tempDir.resolve("reports"));
        Path reportPath = tempDir.resolve("reports").resolve("event-statistics.dat");
        EventStatisticsReport report = new EventStatisticsReport(
                "FirstHourBreach",
                List.of(new EventStatisticsResult("ES", "FirstHourBreach", 10, 3, 2)),
                List.of(new EventStatisticsResult("ESU25", "FirstHourBreach", 10, 3, 2))
        );

        store.save(reportPath, new SavedReport<>("event-statistics", report));
        SavedReport<EventStatisticsReport> loaded = store.load(
                reportPath,
                EventStatisticsReport.class,
                "event-statistics"
        );

        assertEquals("event-statistics", loaded.getReportType());
        assertEquals("FirstHourBreach", loaded.getReport().getEventName());
        assertEquals(1, loaded.getReport().getInstrumentResults().size());
        assertEquals(1, loaded.getReport().getContractResults().size());
        assertTrue(reportPath.toString().endsWith(".dat"));
    }

    @Test
    void saveAndLoadBacktestReportDatFile() {
        GuiReportStore store = new GuiReportStore(tempDir.resolve("reports"));
        Path reportPath = tempDir.resolve("reports").resolve("backtest.dat");
        BacktestResult report = new BacktestResult(
                "OpeningRangeContinuation",
                List.of("ESU25"),
                100,
                4
        );

        store.save(reportPath, new SavedReport<>("backtest", report));
        SavedReport<BacktestResult> loaded = store.load(reportPath, BacktestResult.class, "backtest");

        assertEquals("backtest", loaded.getReportType());
        assertEquals("OpeningRangeContinuation", loaded.getReport().getStrategyName());
        assertEquals(List.of("ESU25"), loaded.getReport().getContractSymbols());
        assertEquals(100, loaded.getReport().getTicksProcessed());
    }

    @Test
    void rejectsWrongReportType() {
        GuiReportStore store = new GuiReportStore(tempDir.resolve("reports"));
        Path reportPath = tempDir.resolve("reports").resolve("event-statistics.dat");
        EventStatisticsReport report = new EventStatisticsReport(
                "FirstHourBreach",
                List.of(),
                List.of()
        );

        store.save(reportPath, new SavedReport<>("event-statistics", report));

        assertThrows(IllegalArgumentException.class,
                () -> store.load(reportPath, EventStatisticsReport.class, "backtest"));
    }

    @Test
    void ensureReportDirectoryCreatesProjectLocalDirectory() {
        Path reportDirectory = tempDir.resolve("runtime").resolve("reports");
        GuiReportStore store = new GuiReportStore(reportDirectory);

        assertEquals(reportDirectory, store.ensureReportDirectory());
        assertTrue(reportDirectory.toFile().isDirectory());
    }
}
