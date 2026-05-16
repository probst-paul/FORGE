package forge.app;

public interface ImportProgressListener {
    ImportProgressListener NO_OP = progress -> {
    };

    void onProgress(ImportProgress progress);
}
