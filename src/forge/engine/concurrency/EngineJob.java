package forge.engine.concurrency;

@FunctionalInterface
public interface EngineJob<T> {
    T run() throws Exception;
}
