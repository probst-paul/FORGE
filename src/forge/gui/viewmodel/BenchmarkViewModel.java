package forge.gui.viewmodel;

public class BenchmarkViewModel extends GuiWorkflowViewModel {
    private String scidFilePath = "";
    private boolean overwriteOverlappingData = true;
    private boolean rebuildDerivedData = true;

    public String getScidFilePath() {
        return scidFilePath;
    }

    public void setScidFilePath(String scidFilePath) {
        this.scidFilePath = scidFilePath == null ? "" : scidFilePath.trim();
    }

    public boolean shouldOverwriteOverlappingData() {
        return overwriteOverlappingData;
    }

    public void setOverwriteOverlappingData(boolean overwriteOverlappingData) {
        this.overwriteOverlappingData = overwriteOverlappingData;
    }

    public boolean shouldRebuildDerivedData() {
        return rebuildDerivedData;
    }

    public void setRebuildDerivedData(boolean rebuildDerivedData) {
        this.rebuildDerivedData = rebuildDerivedData;
    }
}
