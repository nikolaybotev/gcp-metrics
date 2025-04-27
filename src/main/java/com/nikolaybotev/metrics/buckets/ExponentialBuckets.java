package com.nikolaybotev.metrics.buckets;

/**
 * Exponential bucket distribution.
 * <p>
 * Buckets start at <code>{@link #scale} * {@link #growthFactor} ^ (i - 1)</code> where <code>i = 1</code> is the first finite bucket.
 * </p>
 * <p>
 * See <a href="https://cloud.google.com/monitoring/api/ref_v3/rest/v3/TypedValue#exponential">Exponential TypedValue</a>
 * and <a href="https://cloud.google.com/logging/docs/logs-based-metrics/distribution-metrics#bucket_layouts">Histogram buckets</a>
 * for more information.
 * </p>
 * <h2>Example</h2>
 * <ul>
 *     <li>Number of buckets (N, {@link #numFiniteBuckets}) = 4
 *     <li>Linear scale (a, {@link #scale}) = 3
 *     <li>Exponential growth factor (b, {@link #growthFactor}) = 2
 * </ul>
 * <p>
 * <pre>
 * Bounds:     (-INF, 3), [3, 6), [6, 12), [12, 24), [24, 48), [48, +INF)
 *                ^         ^       ^         ^         ^         ^
 * Bucket (i):    0         1       2         3         4         5
 * </pre>
 *
 * @param numFiniteBuckets number of finite buckets
 * @param growthFactor exponential growth factor
 * @param scale linear scale
 */
public record ExponentialBuckets(int numFiniteBuckets, double growthFactor, double scale) implements Buckets {
    @Override
    public int bucketForValue(long value) {
        if (value < scale) {
            return 0;
        }
        return Math.min((int) log(growthFactor, value / scale), numFiniteBuckets) + 1;
    }

    @Override
    public int numFiniteBuckets() {
        return numFiniteBuckets;
    }

    private static double log(double base, double n) {
        return Math.log(n) / Math.log(base);
    }
}
