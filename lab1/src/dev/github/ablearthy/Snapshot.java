package dev.github.ablearthy;

public record Snapshot(
        // exactly 256 elements
        long[] buckets,
        long count,
        long sum,
        long min,
        long max,
        // in milliseconds
        long p50,
        long p99
) {}
