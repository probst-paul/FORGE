package forge.gui;

import forge.gui.controller.MainWindowController;
import javafx.application.Application;
import javafx.stage.Stage;

public class ForgeGuiApplication extends Application {
    /*
     * Intent: Start the JavaFX launcher from a normal Java main method.
     * Precondition: JavaFX runtime must be available on the module/class path.
     * Returns: Nothing.
     * Postcondition: JavaFX creates the application instance and calls start().
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        /*
         * Intent: Build and show the top-level GUI through the GUI facade.
         * Precondition: stage must be supplied by JavaFX.
         * Returns: Nothing.
         * Postcondition: The primary window is rendered and visible to the user.
         */
        MainWindowController controller = FacadeForgeGui.getTheInstance()
                .forgeGuiAccess()
                .createMainWindowController();
        controller.show(stage);
    }

    @Override
    public void stop() {
        /*
         * Intent: Release GUI background workflow resources when JavaFX exits.
         * Precondition: JavaFX is stopping the application.
         * Returns: Nothing.
         * Postcondition: Queued/running GUI workflow tasks are asked to stop.
         */
        FacadeForgeGui.getTheInstance()
                .forgeGuiAccess()
                .shutdownWorkflowRunner();
    }

    /*
     * Intent: Provide a facade-friendly entry point for launching this JavaFX application.
     * Precondition: JavaFX must not already have been launched in the same JVM.
     * Returns: Nothing.
     * Postcondition: JavaFX application startup is delegated to ForgeGuiApplication.
     */
    public static void launchGui(String[] args) {
        Application.launch(ForgeGuiApplication.class, args);
    }
}
