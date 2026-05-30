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
        /*
         * Intent: Run the CLI with console-backed input and output.
         * Precondition: Standard input/output should be available.
         * Returns: Nothing.
         * Postcondition: CLI controller runs until user exit or completion.
         */
        public void run() {
            controller.run();
        }

        /*
         * Intent: Run the CLI with caller-provided input/output adapters.
         * Precondition: Input and output adapters should satisfy their interface contracts.
         * Returns: Nothing.
         * Postcondition: CLI controller runs against the provided adapters, useful for tests or alternate shells.
         */
        public void run(UserInput input, UserOutput output) {
            controller.run(input, output);
        }
    }
}
