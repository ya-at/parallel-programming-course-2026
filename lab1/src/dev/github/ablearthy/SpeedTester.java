package dev.github.ablearthy;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpeedTester {

    private static volatile long blackhole;

    private static final Path RESULTS = Path.of("benchmark-results.csv");

    private static final int VALUES_COUNT = 1 << 20;
    private static final int ROUNDS_COUNT = 5;
    private static final int ROUND_DURATION_SECONDS = 5;

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 2) {
            System.err.println("Usage: <collector-type> <threadsCount>");
            System.exit(1);
        }

        String collectorType = args[0];
        int threadsCount;

        try {
            threadsCount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("The second argument must be an integer: " + args[1]);
            System.exit(1);
            return;
        }

        MetricsCollector collector = MetricsCollectorUtils.createCollector(collectorType);

        final var collectorName = collector.getClass().getSimpleName();
        System.out.printf("Running %s with threadsCount = %d%n", collectorName, threadsCount);
        var values = StatUtils.generate(VALUES_COUNT);
        final var opsPerSecond = measure(collector, values, threadsCount);

        if (Files.notExists(RESULTS)) {
            Files.writeString(
                    RESULTS,
                    "collector,threads,ops_per_second\n",
                    StandardOpenOption.CREATE
            );
        }

        Files.writeString(
                RESULTS,
                String.format("%s,%d,%.3f%n", collectorName, threadsCount, opsPerSecond),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

    }

    private static double measure(MetricsCollector collector, long[] values, int threadsCount) throws InterruptedException {
        System.out.printf("Warmup%n");
        runOnce(collector, values, threadsCount, ROUND_DURATION_SECONDS);

        var results = new double[5];

        for (int i = 0; i < ROUNDS_COUNT; i++) {
            System.out.printf("Starting round %d / %d%n", i + 1, ROUNDS_COUNT);
            results[i] = runOnce(collector, values, threadsCount, ROUND_DURATION_SECONDS);
        }
        var snapshot = collector.snapshot();
        blackhole = snapshot.count();
        System.out.printf("%s%n", snapshot);

        var opsPerSecond = median(results);
        System.out.printf("opsPerSecond = %.3f%n", opsPerSecond);
        return opsPerSecond;
    }

    private static double runOnce(MetricsCollector collector, long[] values, int threadsCount, int durationSeconds) throws InterruptedException {
        var start = new CountDownLatch(1);
        var stop = new AtomicBoolean(false);

        var ops = new long[threadsCount];
        var workers = new Thread[threadsCount];

        for (int k = 0; k < threadsCount; k++) {
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

        long startTime = System.nanoTime();
        start.countDown();
        Thread.sleep(durationSeconds * 1000L);
        stop.set(true);
        long stopTime = System.nanoTime();
        for (Thread worker : workers) {
            worker.join();
        }
        long totalOps = 0;
        for (long op : ops) {
            totalOps += op;
        }

        var elapsedSeconds = (stopTime - startTime) / 1_000_000_000d;

        return totalOps / elapsedSeconds;
    }

    private static double median(double[] values) {
        var copy = values.clone();
        Arrays.sort(copy);

        int n = copy.length;

        if (n % 2 == 1) {
            return copy[n / 2];
        }

        return (copy[n / 2 - 1] + copy[n / 2]) / 2;
    }
}
