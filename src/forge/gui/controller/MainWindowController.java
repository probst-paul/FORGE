package forge.gui.controller;

import forge.gui.view.MainWindowView;
import forge.gui.viewmodel.MainWindowViewModel;
import javafx.stage.Stage;

public class MainWindowController {
    private final MainWindowViewModel viewModel;
    private final MainWindowView view;

    public MainWindowController() {
        this(new MainWindowViewModel(), new MainWindowView());
    }

    public MainWindowController(MainWindowViewModel viewModel, MainWindowView view) {
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        if (view == null) {
            throw new IllegalArgumentException("view is required");
        }
        this.viewModel = viewModel;
        this.view = view;
    }

    public void show() {
        /*
         * Intent: Render the non-JavaFX placeholder output used by lightweight callers/tests.
         * Precondition: View and view model must be initialized.
         * Returns: Nothing.
         * Postcondition: The view receives a ready status message.
         */
        viewModel.setStatusMessage("JavaFX GUI placeholder is ready.");
        view.render(viewModel);
    }

    public void show(Stage stage) {
        /*
         * Intent: Render the full JavaFX main window.
         * Precondition: stage must be supplied by JavaFX.
         * Returns: Nothing.
         * Postcondition: Main navigation and default workflow content are displayed.
         */
        if (stage == null) {
            throw new IllegalArgumentException("stage is required");
        }
        viewModel.setStatusMessage("Ready.");
        view.render(stage, viewModel);
    }
}
