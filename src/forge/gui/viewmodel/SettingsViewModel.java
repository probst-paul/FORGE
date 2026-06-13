package forge.gui.viewmodel;

public class SettingsViewModel extends GuiWorkflowViewModel {
    private int droppedTableCount;

    public int getDroppedTableCount() {
        return droppedTableCount;
    }

    public void setDroppedTableCount(int droppedTableCount) {
        if (droppedTableCount < 0) {
            throw new IllegalArgumentException("droppedTableCount cannot be negative");
        }
        this.droppedTableCount = droppedTableCount;
    }
}
