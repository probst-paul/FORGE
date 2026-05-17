package forge.app;

public interface BacktestProgressListener {
    BacktestProgressListener NO_OP = progress -> {
    };

    void onProgress(BacktestProgress progress);
}
