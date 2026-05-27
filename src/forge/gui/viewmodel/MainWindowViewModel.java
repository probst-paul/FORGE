package forge.gui.viewmodel;

public class MainWindowViewModel extends GuiWorkflowViewModel {
    private GuiWorkflowType activeWorkflow = GuiWorkflowType.IMPORT_DATA;
    private String windowTitle = "FORGE";

    public GuiWorkflowType getActiveWorkflow() {
        return activeWorkflow;
    }

    public void setActiveWorkflow(GuiWorkflowType activeWorkflow) {
        if (activeWorkflow == null) {
            throw new IllegalArgumentException("activeWorkflow is required");
        }
        this.activeWorkflow = activeWorkflow;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle == null || windowTitle.trim().isEmpty()
                ? "FORGE"
                : windowTitle.trim();
    }
}
