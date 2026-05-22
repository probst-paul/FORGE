package forge.data.build;

public interface DataBuildProgressListener {
    DataBuildProgressListener NO_OP = progress -> {
    };

    void onProgress(DataBuildProgress progress);
}
