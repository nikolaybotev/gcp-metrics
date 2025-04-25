package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.nikolaybotev.metrics.buckets.Buckets;

import java.io.Serializable;
import java.util.Arrays;

public class HistogramBucketAggregator {
    private final Buckets bucketsDefinition;

    private final long[] buckets;

    private long numSamples;
    private double mean;
    private double sumOfSquaredDeviation;

    private final Object lock = new Serializable() {};

    public HistogramBucketAggregator(Buckets bucketsDefinition) {
        this.bucketsDefinition = bucketsDefinition;

        this.buckets = new long[bucketsDefinition.count() + 2];
    }

    public void add(long value) {
        synchronized (lock) {
            // Update bucket
            var bucket = bucketsDefinition.bucketForValue(value);
            buckets[bucket] += 1;

            // Update count, mean and M2 using Welford's method for accumulating the sum of squared deviations.
            numSamples = numSamples + 1;
            var delta = value - mean;
            mean = mean + (delta / numSamples);
            sumOfSquaredDeviation = sumOfSquaredDeviation + delta * (value - mean);
        }
    }

    public HistogramBuckets getAndClear() {
        synchronized (lock) {
            // Make a copy
            var result = new HistogramBuckets(
                    Arrays.copyOf(buckets, buckets.length), numSamples, mean, sumOfSquaredDeviation);

            // Clear
            Arrays.fill(buckets, 0);
            numSamples = 0;
            mean = 0;
            sumOfSquaredDeviation = 0;

            return result;
        }
    }
}
