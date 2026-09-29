package dev.github.ablearthy;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.List;
import java.util.ArrayList;

class ThreadLocalMetricsCollector implements MetricsCollector {

    static final class ThreadState {
        final AtomicLongArray buckets = new AtomicLongArray(256);
        final AtomicLong count = new AtomicLong(0);
        final AtomicLong sum = new AtomicLong(0);
        final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong max = new AtomicLong(0);
    }

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

        state.buckets.setRelease(bucket, state.buckets.getPlain(bucket) + 1);
        state.count.setRelease(state.count.getPlain() + 1);
        state.sum.setRelease(state.sum.getPlain() + value);

        if (value < state.min.getPlain()) {
            state.min.setRelease(value);
        }
        if (value > state.max.getPlain()) {
            state.max.setRelease(value);
        }
    }

    @Override
    public Snapshot snapshot() {
        long[] copy = new long[256];
        long count = 0, sum = 0, min = Long.MAX_VALUE, max = 0;

        synchronized (listLock) {
            for (ThreadState s : allStates) {
                for (int i = 0; i < s.buckets.length(); i++) copy[i] += s.buckets.get(i);
                count += s.count.get();

                sum += s.sum.get();
                min = Math.min(min, s.min.get());
                max = Math.max(max, s.max.get());
            }
        }


        long p50 = StatUtils.percentile(copy, count, 0.50);
        long p99 = StatUtils.percentile(copy, count, 0.99);

        return new Snapshot(copy, count, sum, min, max, p50, p99);
    }
}
