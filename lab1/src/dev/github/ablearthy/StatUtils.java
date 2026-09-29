package dev.github.ablearthy;

import java.util.Random;

public class StatUtils {
    private static final long SEED = 42L;
    private static final double ZIPF_EXPONENT = 1.15;
    private static final int NUMBERS_COUNT = 1023;

    public static long[] generate(int count) {
        var ks = new double[NUMBERS_COUNT];

        double sum = 0;
        for (int i = 0; i < ks.length; i++) {
            ks[i] = 1.0 / Math.pow(i + 1, ZIPF_EXPONENT);
            sum += ks[i];
        }
        for (int i = 0; i < ks.length; i++) {
            ks[i] /= sum;
            if (i != 0) {
                ks[i] += ks[i - 1];
            }
        }
        ks[ks.length - 1] = 1.0;

        var result = new long[count];
        var random = new Random(SEED);

        int in_first_bucket_count = 0;

        for (int i = 0; i < result.length; i++) {
            var r = random.nextDouble();
            for (int j = 0; j < ks.length; j++) {
                if (ks[j] >= r) {
                    result[i] = j + 1;
                    break;
                }
            }
            if (result[i] <= 3) {
                in_first_bucket_count++;
            }
        }
        System.out.printf("in_first_bucket: %.3f%%%n", 100.0 * in_first_bucket_count / result.length);

        return result;
    }

    public static long percentile(long[] buckets, long count, double percentile) {
        if (count == 0) {
            return 0;
        }

        var threshold = (long) Math.ceil(count * percentile);

        var cumulative = 0L;
        for (int i = 0; i < buckets.length; i++) {
            cumulative += buckets[i];

            if (cumulative >= threshold) {
                return i * 4L;
            }
        }

        return 255 * 4L;
    }
}
