package dev.github.ablearthy;

public interface MetricsCollector {
    void record(long value);
    Snapshot snapshot();
}
