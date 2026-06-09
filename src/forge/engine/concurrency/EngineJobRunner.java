package forge.engine.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class EngineJobRunner {
    private static final int DEFAULT_PARALLELISM = 4;

    private final int parallelism;
    private final String threadNamePrefix;

    public EngineJobRunner(String threadNamePrefix) {
        this(threadNamePrefix, DEFAULT_PARALLELISM);
    }

    public EngineJobRunner(String threadNamePrefix, int parallelism) {
        /*
         * Intent: Create a bounded job runner for independent engine work.
         * Precondition: Thread prefix must be present and parallelism must be positive.
         * Returns: A constructed EngineJobRunner instance.
         * Postcondition: Future jobs run on named daemon worker threads.
         */
        if (threadNamePrefix == null || threadNamePrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("threadNamePrefix is required");
        }
        if (parallelism < 1) {
            throw new IllegalArgumentException("parallelism must be positive");
        }
        this.threadNamePrefix = threadNamePrefix.trim();
        this.parallelism = parallelism;
    }

    public <T> List<T> runAll(List<? extends EngineJob<T>> jobs) {
        /*
         * Intent: Execute independent engine jobs concurrently and collect results in submission order.
         * Precondition: Jobs must be non-null and safe to run independently.
         * Returns: Immutable job result list.
         * Postcondition: All submitted jobs have completed or the first failure is surfaced.
         */
        if (jobs == null) {
            throw new IllegalArgumentException("jobs is required");
        }
        if (jobs.isEmpty()) {
            return Collections.emptyList();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(
                Math.min(parallelism, jobs.size()),
                new EngineJobThreadFactory(threadNamePrefix)
        );
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (EngineJob<T> job : jobs) {
                if (job == null) {
                    throw new IllegalArgumentException("jobs cannot contain null values");
                }
                futures.add(executorService.submit(job::run));
            }

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(await(future));
            }
            return Collections.unmodifiableList(results);
        } finally {
            executorService.shutdownNow();
        }
    }

    private <T> T await(Future<T> future) {
        /*
         * Intent: Convert Future completion into a result or application exception.
         * Precondition: Future must come from this runner.
         * Returns: Completed job result.
         * Postcondition: Interrupted callers have their interrupt flag restored.
         */
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Engine job run was interrupted", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Engine job failed", cause);
        }
    }

    private static class EngineJobThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger sequence = new AtomicInteger(1);

        private EngineJobThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            /*
             * Intent: Name engine worker threads by workflow lane.
             * Precondition: Runnable must be supplied by the executor.
             * Returns: Daemon worker thread.
             * Postcondition: Thread is ready for executor-managed execution.
             */
            Thread thread = new Thread(runnable, prefix + "-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
