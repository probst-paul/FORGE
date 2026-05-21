package forge.app;

public interface EventStatisticsProgressListener {
    EventStatisticsProgressListener NO_OP = progress -> {
    };

    void onProgress(EventStatisticsProgress progress);
}
