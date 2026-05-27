package forge.gui.view;

import forge.gui.viewmodel.MainWindowViewModel;

public class MainWindowView {
    public void render(MainWindowViewModel viewModel) {
        if (viewModel == null) {
            throw new IllegalArgumentException("viewModel is required");
        }
        System.out.println(viewModel.getStatusMessage());
    }
}
