package forge.benchmark;

public class FacadeForgeBenchmark {
    private static final FacadeForgeBenchmark THE_INSTANCE = new FacadeForgeBenchmark();

    private final BenchmarkWorkflow benchmarkWorkflow;
    private final ForgeBenchmarkAccess access = new ForgeBenchmarkAccess();

    public FacadeForgeBenchmark() {
        /*
         * Intent: Create the benchmark facade with the default benchmark workflow.
         * Precondition: BenchmarkWorkflow default dependencies must be available.
         * Returns: A constructed FacadeForgeBenchmark instance.
         * Postcondition: Benchmark access object can run benchmark workflows through one package-level entry point.
         */
        this(new BenchmarkWorkflow());
    }

    public FacadeForgeBenchmark(BenchmarkWorkflow benchmarkWorkflow) {
        /*
         * Intent: Create the benchmark facade with an explicit workflow dependency.
         * Precondition: Benchmark workflow must exist.
         * Returns: A constructed FacadeForgeBenchmark instance.
         * Postcondition: Facade delegates benchmark execution to the supplied workflow.
         */
        if (benchmarkWorkflow == null) {
            throw new IllegalArgumentException("benchmarkWorkflow is required");
        }
        this.benchmarkWorkflow = benchmarkWorkflow;
    }

    public static FacadeForgeBenchmark getTheInstance() {
        return THE_INSTANCE;
    }

    public ForgeBenchmarkAccess forgeBenchmarkAccess() {
        return access;
    }

    public class ForgeBenchmarkAccess {
        /*
         * Intent: Run a benchmark workflow without exposing benchmark internals to callers.
         * Precondition: Benchmark request must exist and satisfy workflow validation.
         * Returns: BenchmarkRunResult produced by the benchmark workflow.
         * Postcondition: Facade state is unchanged.
         */
        public BenchmarkRunResult runBenchmark(BenchmarkRunRequest request) {
            return benchmarkWorkflow.run(request);
        }
    }
}
