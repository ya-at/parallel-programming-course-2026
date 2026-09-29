package dev.github.ablearthy;

public class SyncMetricsCollectorWrapper implements MetricsCollector {
    private final SyncMetricsCollector underlying = new SyncMetricsCollector();

    @Override
    public synchronized void record(long value) {
        underlying.record(value);
    }

    @Override
    public synchronized Snapshot snapshot() {
        return underlying.snapshot();
    }
}
