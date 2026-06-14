package forge.app;

import forge.gui.FacadeForgeGui;

public class Main {
    /*
     * Intent: Start the JavaFX GUI application through its package facade.
     * Precondition: JavaFX runtime and GUI facade singleton must be available.
     * Returns: Nothing.
     * Postcondition: GUI workflow runs until the application exits.
     */
    public static void main(String[] args) {
        FacadeForgeGui.getTheInstance().forgeGuiAccess().launch(args);
    }
}
