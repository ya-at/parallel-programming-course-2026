package dev.github.ablearthy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

class DoubleBufferingMetricsCollector implements MetricsCollector {

    private final long[] globalBuckets = new long[256];

    private long globalCount = 0;
    private long globalSum = 0;
    private long globalMin = Long.MAX_VALUE;
    private long globalMax = 0;

    static final class ThreadState {
        final long[][] buckets = new long[2][256];
        final long[] count = new long[2];
        final long[] sum = new long[2];
        final long[] min = {Long.MAX_VALUE, Long.MAX_VALUE};
        final long[] max = {0, 0};

        // -1 = nowhere, 0 = write to buffer 0, 1 = write to buffer 1
        final AtomicInteger inside = new AtomicInteger(-1);
    }

    private volatile int active = 0;
    private final List<ThreadState> allStates = new ArrayList<>();
    private final Object listLock = new Object();

    private final ThreadLocal<ThreadState> myState = ThreadLocal.withInitial(() -> {
        ThreadState s = new ThreadState();
        synchronized (listLock) {
            allStates.add(s);
        }
        return s;
    });

    @Override
    public void record(long value) {
        final var state = myState.get();
        final var bucket = (int) Math.min(value / 4, 255);

        int b = -1;

        while (true) {
            b = active;
            state.inside.set(b);
            if (active == b) {
                break;
            }
            state.inside.set(-1);
        }

        state.buckets[b][bucket]++;
        state.count[b]++;
        state.sum[b] += value;
        if (value < state.min[b]) {
            state.min[b] = value;
        }
        if (value > state.max[b]) {
            state.max[b] = value;
        }

        state.inside.setRelease(-1);
    }

    @Override
    public Snapshot snapshot() {
        synchronized (listLock) {
            int old = active;
            active = 1 - old;

            for (ThreadState s : allStates) {
                while (s.inside.get() == old) {
                    Thread.onSpinWait();
                }

                for (int i = 0; i < s.buckets[old].length; i++) {
                    globalBuckets[i] += s.buckets[old][i];
                }
                globalCount += s.count[old];
                globalSum += s.sum[old];
                globalMin = Math.min(globalMin, s.min[old]);
                globalMax = Math.max(globalMax, s.max[old]);

                s.count[old] = 0;
                s.sum[old] = 0;
                Arrays.fill(s.buckets[old], 0);
                s.min[old] = Long.MAX_VALUE;
                s.max[old] = 0;
            }

            long[] copy = globalBuckets.clone();
            long p50 = StatUtils.percentile(copy, globalCount, 0.50);
            long p99 = StatUtils.percentile(copy, globalCount, 0.99);
            return new Snapshot(copy, globalCount, globalSum, globalMin, globalMax, p50, p99);
        }
    }
}
