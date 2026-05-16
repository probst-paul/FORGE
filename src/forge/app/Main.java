package forge.app;

import forge.cli.FacadeForgeCli;

public class Main {
    public static void main(String[] args) {
        FacadeForgeCli.getTheInstance().forgeCliAccess().run();
    }
}
