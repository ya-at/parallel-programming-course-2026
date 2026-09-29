package dev.github.ablearthy;

public class SyncMetricsCollector implements MetricsCollector {

    private final long[] buckets = new long[256];

    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max = 0;

    @Override
    public void record(long value) {
        final var bucket = (int) Math.min(value / 4, 255);
        buckets[bucket]++;
        count++;
        sum += value;
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
    }

    @Override
    public Snapshot snapshot() {
        var copy = buckets.clone();
        var p50 = StatUtils.percentile(copy, count, 0.50);
        var p99 = StatUtils.percentile(copy, count, 0.99);

        return new Snapshot(copy, count, sum, min, max, p50, p99);
    }

}
