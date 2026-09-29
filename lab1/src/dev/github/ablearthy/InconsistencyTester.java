package dev.github.ablearthy;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class InconsistencyTester {
    private static final int VALUES_COUNT = 1 << 20;
    private static final int WORKERS_COUNT = 4;

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 1) {
            System.err.println("Usage: <collector-type>");
            System.exit(1);
        }
        String collectorType = args[0];
        MetricsCollector collector = MetricsCollectorUtils.createCollector(collectorType);

        var values = StatUtils.generate(VALUES_COUNT);

        var ops = new long[WORKERS_COUNT];
        var workers = new Thread[WORKERS_COUNT];

        var start = new CountDownLatch(1);
        var stop = new AtomicBoolean(false);

        for (int k = 0; k < workers.length; k++) {
            final int threadIndex = k;

            workers[k] = new Thread(() -> {
                long localCount = 0;
                int i = (threadIndex * 1000) % values.length;

                try {
                    start.await();
                    while (!stop.get()) {
                        collector.record(values[i]);
                        localCount++;
                        i++;
                        if (i == values.length) {
                            i = 0;
                        }
                    }
                    ops[threadIndex] = localCount;
                } catch (InterruptedException e) {
                    /* ignore */
                }
            });

            workers[k].start();
        }

        start.countDown();
        int consistentHitsCount = 0;
        int incosistentSumLessThanCount = 0;
        int incosistentSumGreaterThanCount = 0;
        int totalHitsCount = 0;

        for (int i = 0; i < 10_000; i++) {
            final var snapshot = collector.snapshot();
            long bucketsSum = 0;
            final var count = snapshot.count();
            for (long b : snapshot.buckets()) {
                bucketsSum += b;
            }
            if (bucketsSum == count) {
                consistentHitsCount++;
            } else if (bucketsSum < count) {
                incosistentSumLessThanCount++;
            } else {
                incosistentSumGreaterThanCount++;
            }

            totalHitsCount++;
        }

        stop.set(true);

        for (Thread worker : workers) {
            worker.join();
        }
        long totalOps = 0;
        for (long op : ops) {
            totalOps += op;
        }

        final var snapshot = collector.snapshot();

        System.out.printf("[threads = %d] Consistent %d out of %d, (sum < count) = %d, (sum > count) = %d%n", workers.length, consistentHitsCount, totalHitsCount, incosistentSumLessThanCount, incosistentSumGreaterThanCount);
        System.out.printf("Sum of local counters = %d, snapshot count = %d%n", totalOps, snapshot.count());

    }
}
