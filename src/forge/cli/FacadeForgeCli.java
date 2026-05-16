package forge.cli;

import forge.app.UserInput;
import forge.app.UserOutput;

public class FacadeForgeCli {
    private static final FacadeForgeCli THE_INSTANCE = new FacadeForgeCli();

    private final ForgeCliAccess access = new ForgeCliAccess();
    private final CliApplicationController controller = new CliApplicationController();

    public static FacadeForgeCli getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeCliAccess forgeCliAccess() {
        return access;
    }

    public class ForgeCliAccess {
        public void run() {
            controller.run();
        }

        public void run(UserInput input, UserOutput output) {
            controller.run(input, output);
        }
    }
}
