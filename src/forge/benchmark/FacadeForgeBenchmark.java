package forge.benchmark;

public class FacadeForgeBenchmark {
    private static final FacadeForgeBenchmark THE_INSTANCE = new FacadeForgeBenchmark();

    private final BenchmarkWorkflow benchmarkWorkflow;
    private final ForgeBenchmarkAccess access = new ForgeBenchmarkAccess();

    public FacadeForgeBenchmark() {
        this(new BenchmarkWorkflow());
    }

    public FacadeForgeBenchmark(BenchmarkWorkflow benchmarkWorkflow) {
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
        public BenchmarkRunResult runBenchmark(BenchmarkRunRequest request) {
            return benchmarkWorkflow.run(request);
        }
    }
}
