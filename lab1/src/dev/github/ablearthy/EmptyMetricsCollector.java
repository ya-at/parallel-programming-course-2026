package dev.github.ablearthy;

public class EmptyMetricsCollector implements MetricsCollector {
    private final SyncMetricsCollector underlying = new SyncMetricsCollector();

    @Override
    public synchronized void record(long value) {}

    @Override
    public synchronized Snapshot snapshot() {
        return underlying.snapshot();
    }
}
