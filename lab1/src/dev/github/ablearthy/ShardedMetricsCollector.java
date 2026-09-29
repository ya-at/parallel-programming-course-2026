package dev.github.ablearthy;

import java.util.concurrent.atomic.AtomicLong;


class ShardedMetricsCollector implements MetricsCollector {

    private final long[][] buckets = new long[16][16];

    private AtomicLong count = new AtomicLong(0);
    private AtomicLong sum = new AtomicLong(0);
    private AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private AtomicLong max = new AtomicLong(0);

    @Override
    public void record(long value) {
        final var bucket = (int) Math.min(value / 4, 255);
        final var idx = bucket % 16;
        synchronized (buckets[idx]) {
            buckets[idx][bucket / 16]++;
        }
        count.getAndIncrement();
        sum.getAndAdd(value);
        while (true) {
            final var oldValue = min.get();
            final long newValue = Math.min(oldValue, value);
            if (min.compareAndSet(oldValue, newValue)) {
                break;
            }
        }

        while (true) {
            final var oldValue = max.get();
            final long newValue = Math.max(oldValue, value);
            if (max.compareAndSet(oldValue, newValue)) {
                break;
            }
        }
    }

    @Override
    public Snapshot snapshot() {
        final long[] copy = new long[256];

        for (int i = 0; i < buckets.length; i++) {
            synchronized (buckets[i]) {
                for (int j = 0; j < buckets[i].length; j++) {
                    copy[i + 16 * j] = buckets[i][j];
                }
            }
        }
        final long countCopy = count.get();
        final long sumCopy = sum.get();
        final long minCopy = min.get();
        final long maxCopy = max.get();

        final long p50 = StatUtils.percentile(copy, countCopy, 0.50);
        final long p99 = StatUtils.percentile(copy, countCopy, 0.99);

        return new Snapshot(copy, countCopy, sumCopy, minCopy, maxCopy, p50, p99);
    }

}
