package forge.gui;

import forge.gui.controller.MainWindowController;

public class ForgeGuiApplication {
    public static void main(String[] args) {
        FacadeForgeGui.getTheInstance().forgeGuiAccess().launch(args);
    }

    public static void launchGui(String[] args) {
        MainWindowController controller = FacadeForgeGui.getTheInstance()
                .forgeGuiAccess()
                .createMainWindowController();
        controller.show();
    }
}
