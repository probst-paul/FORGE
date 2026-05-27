package forge.gui;

import forge.gui.controller.MainWindowController;
import javafx.application.Application;
import javafx.stage.Stage;

public class ForgeGuiApplication extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        MainWindowController controller = FacadeForgeGui.getTheInstance()
                .forgeGuiAccess()
                .createMainWindowController();
        controller.show(stage);
    }

    public static void launchGui(String[] args) {
        Application.launch(ForgeGuiApplication.class, args);
    }
}
