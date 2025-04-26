package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.nikolaybotev.metrics.buckets.Buckets;

import java.io.Serializable;
import java.util.Arrays;

public class HistogramAggregatorBuffer implements HistogramAggregator {
    private final Buckets bucketsDefinition;

    private final long[] buckets;

    private long numSamples;
    private double mean;
    private double sumOfSquaredDeviation;

    private final Object lock = new Serializable() {};

    public HistogramAggregatorBuffer(Buckets bucketsDefinition) {
        this.bucketsDefinition = bucketsDefinition;

        this.buckets = new long[bucketsDefinition.count() + 2];
    }

    @Override
    public void add(long value) {
        // no-op
    }

    @Override
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
