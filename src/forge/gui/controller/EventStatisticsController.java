package forge.gui.controller;

import forge.app.EventStatisticsRequest;
import forge.app.FacadeForgeApplication;
import forge.data.FacadeForgeData;
import forge.data.catalog.InstrumentDataCatalog.AvailableContractData;
import forge.data.market.ContractTradeWindow;
import forge.engine.EventStatisticsReport;
import forge.gui.viewmodel.EventStatisticsViewModel;
import forge.gui.viewmodel.GuiProgressBindings;
import forge.study.FacadeForgeStudy;
import forge.study.MarketStudy;
import javafx.concurrent.Task;

import java.util.List;

public class EventStatisticsController {
    private final FacadeForgeApplication forgeApplication;
    private final FacadeForgeData forgeData;
    private final FacadeForgeStudy forgeStudy;
    private final EventStatisticsViewModel viewModel;

    public EventStatisticsController() {
        this(
                FacadeForgeApplication.getTheInstance(),
                FacadeForgeData.getTheInstance(),
                FacadeForgeStudy.getTheInstance(),
                new EventStatisticsViewModel()
        );
    }

    public EventStatisticsController(FacadeForgeApplication forgeApplication, EventStatisticsViewModel viewModel) {
        this(
                forgeApplication,
                FacadeForgeData.getTheInstance(),
                FacadeForgeStudy.getTheInstance(),
                viewModel
        );
    }

    public EventStatisticsController(
            FacadeForgeApplication forgeApplication,
            FacadeForgeData forgeData,
            FacadeForgeStudy forgeStudy,
            EventStatisticsViewModel viewModel
    ) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (forgeData == null) {
            throw new IllegalArgumentException("forgeData is required");
        }
        if (forgeStudy == null) {
            throw new IllegalArgumentException("forgeStudy is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
        this.forgeData = forgeData;
        this.forgeStudy = forgeStudy;
        this.viewModel = viewModel;
    }

    public EventStatisticsViewModel getViewModel() {
        return viewModel;
    }

    public List<AvailableContractData> getAvailableContracts() {
        return forgeData.forgeDataAccess().getAvailableContracts();
    }

    public List<String> getSupportedStudyNames() {
        return forgeStudy.forgeStudyAccess().getSupportedStudyNames();
    }

    public MarketStudy getStudy(String studyName) {
        return forgeStudy.forgeStudyAccess().getStudy(studyName);
    }

    public EventStatisticsReport runEventStatistics(List<ContractTradeWindow> contractWindows, String eventName) {
        /*
         * Intent: Run event statistics synchronously for tests or non-task GUI callers.
         * Precondition: contractWindows must be selected and eventName must name a supported study/event.
         * Returns: Completed event statistics report.
         * Postcondition: The view model is marked succeeded or failed with result counts.
         */
        viewModel.setContractWindows(contractWindows);
        viewModel.setEventName(eventName);
        viewModel.markStarted("Running event statistics...");
        try {
            EventStatisticsReport report = forgeApplication.forgeApplicationAccess().runEventStatistics(
                    new EventStatisticsRequest(
                            contractWindows,
                            eventName,
                            GuiProgressBindings.eventStatisticsProgress(viewModel, "Running event statistics...")
                    )
            );
            applyEventStatisticsReport(report);
            return report;
        } catch (RuntimeException exception) {
            viewModel.markFailed("Could not run event statistics.", exception);
            throw exception;
        }
    }

    public Task<EventStatisticsReport> runEventStatisticsTask(
            List<ContractTradeWindow> contractWindows,
            String eventName
    ) {
        /*
         * Intent: Create a JavaFX task for event statistics off the UI thread.
         * Precondition: contractWindows must be selected and eventName must name a supported study/event.
         * Returns: Task that yields the event statistics report.
         * Postcondition: Task progress callbacks update the view model through JavaFX bindings.
         */
        viewModel.setContractWindows(contractWindows);
        viewModel.setEventName(eventName);
        return GuiControllerTasks.create(
                viewModel,
                "Running event statistics...",
                "Could not run event statistics.",
                task -> forgeApplication.forgeApplicationAccess().runEventStatistics(
                        new EventStatisticsRequest(
                                contractWindows,
                                eventName,
                                GuiProgressBindings.eventStatisticsProgress(task, "Running event statistics...")
                        )
                ),
                this::applyEventStatisticsReport
        );
    }

    private void applyEventStatisticsReport(EventStatisticsReport report) {
        /*
         * Intent: Copy completed event statistics counts into GUI state.
         * Precondition: report must be a successful statistics report.
         * Returns: Nothing.
         * Postcondition: The view model exposes instrument/contract result counts and success text.
         */
        viewModel.setInstrumentResultCount(report.getInstrumentResults().size());
        viewModel.setContractResultCount(report.getContractResults().size());
        viewModel.markSucceeded(
                "Event statistics complete.",
                "Built " + report.getInstrumentResults().size() + " instrument result(s) and "
                        + report.getContractResults().size() + " contract result(s)."
        );
    }
}
