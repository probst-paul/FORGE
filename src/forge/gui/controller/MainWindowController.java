package forge.gui.controller;

import forge.gui.view.MainWindowView;
import forge.gui.viewmodel.MainWindowViewModel;

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
        viewModel.setStatusMessage("JavaFX GUI placeholder is ready.");
        view.render(viewModel);
    }
}
