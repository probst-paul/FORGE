package forge.app;

import forge.cli.FacadeForgeCli;

public class Main {
    /*
     * Intent: Start the CLI application through its package facade.
     * Precondition: CLI facade singleton must be available.
     * Returns: Nothing.
     * Postcondition: CLI workflow runs until completion or user exit.
     */
    public static void main(String[] args) {
        FacadeForgeCli.getTheInstance().forgeCliAccess().run();
    }
}
