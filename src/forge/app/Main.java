package forge.app;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        FacadeForgeApplication forgeApplication = FacadeForgeApplication.getTheInstance();
        forgeApplication.forgeApplicationAccess().runBacktestSetup(
                new ConsoleUserInput(new Scanner(System.in)),
                new ConsoleUserOutput()
        );
    }
}
