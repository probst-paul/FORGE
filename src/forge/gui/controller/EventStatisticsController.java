package forge.gui.controller;

import forge.app.EventStatisticsRequest;
import forge.app.FacadeForgeApplication;
import forge.data.market.ContractTradeWindow;
import forge.engine.EventStatisticsReport;
import forge.gui.viewmodel.EventStatisticsViewModel;
import forge.gui.viewmodel.GuiProgressBindings;
import javafx.concurrent.Task;

import java.util.List;

public class EventStatisticsController {
    private final FacadeForgeApplication forgeApplication;
    private final EventStatisticsViewModel viewModel;

    public EventStatisticsController() {
        this(FacadeForgeApplication.getTheInstance(), new EventStatisticsViewModel());
    }

    public EventStatisticsController(FacadeForgeApplication forgeApplication, EventStatisticsViewModel viewModel) {
        if (forgeApplication == null) {
            throw new IllegalArgumentException("forgeApplication is required");
        }
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        this.forgeApplication = forgeApplication;
        this.viewModel = viewModel;
    }

    public EventStatisticsViewModel getViewModel() {
        return viewModel;
    }

    public EventStatisticsReport runEventStatistics(List<ContractTradeWindow> contractWindows, String eventName) {
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
        viewModel.setInstrumentResultCount(report.getInstrumentResults().size());
        viewModel.setContractResultCount(report.getContractResults().size());
        viewModel.markSucceeded(
                "Event statistics complete.",
                "Built " + report.getInstrumentResults().size() + " instrument result(s) and "
                        + report.getContractResults().size() + " contract result(s)."
        );
    }
}
