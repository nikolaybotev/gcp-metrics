package com.nikolaybotev.metrics.buckets;

import java.io.Serial;
import java.util.Arrays;

/**
 * Linear bucket distribution.
 * <p>
 * Buckets start at <code>{@link #offset} + {@link #width} * (i - 1)</code> where <code>i = 1</code> is the first
 * finite bucket.
 * <p>
 * See <a href="https://cloud.google.com/monitoring/api/ref_v3/rest/v3/TypedValue#linear">Linear TypedValue</a>
 * and <a href="https://cloud.google.com/logging/docs/logs-based-metrics/distribution-metrics#bucket_layouts">Histogram buckets</a>
 * for more information.
 *
 * <h2>Example</h2>
 *
 * <ul>
 *     <li>Start value (a, {@link #offset}) = 0
 *     <li>Number of buckets (N, {@link #numFiniteBuckets}) = 3
 *     <li>Bucket size (b, {@link #width}) = 10
 * </ul>
 *
 * <p>
 *
 * <pre>
 *     Bounds:     (-INF, 0), [0, 10), [10, 20), [20, 30), [30, +INF)
 *                    ^         ^         ^         ^         ^
 *     Bucket (i):    0         1         2         3         4
 * </pre>
 *
 * @param numFiniteBuckets number of finite buckets
 * @param width size of each bucket
 * @param offset start of first finite bucket (this value is subtracted from values displayed in Google Cloud
 *              Monitoring charts)
 */
public record LinearBuckets(double offset, double width, int numFiniteBuckets) implements Buckets {
    @Serial
    private static final long serialVersionUID = -4022957706568722400L;

    @Override
    public int bucketForValue(long value) {
        return (int) Math.min(Math.max(0, (value - offset + width) / width), numFiniteBuckets + 1);
    }

    public ExplicitBuckets toExplicitBuckets() {
        var bounds = new double[numFiniteBuckets + 1];
        Arrays.setAll(bounds, i -> offset + width * i);
        return new ExplicitBuckets(bounds);
    }
}
