package dev.github.ablearthy;

public class MetricsCollectorUtils {
    public static MetricsCollector createCollector(String collectorType) {
        return switch (collectorType) {
            case "sync" -> new SyncMetricsCollector();
            case "mutex" -> new SyncMetricsCollectorWrapper();
            case "sharded" -> new ShardedMetricsCollector();
            case "empty" -> new EmptyMetricsCollector();
            case "thread-local" -> new ThreadLocalMetricsCollector();
            case "double-buffering" -> new DoubleBufferingMetricsCollector();
            default -> throw new IllegalArgumentException("Unknown collector type: " + collectorType);
        };
    }
}
