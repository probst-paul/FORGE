package forge.app;

public class UserQuitException extends RuntimeException {
    public UserQuitException() {
        super("User requested application exit");
    }
}
