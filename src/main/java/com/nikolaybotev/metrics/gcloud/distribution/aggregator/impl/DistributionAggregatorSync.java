package com.nikolaybotev.metrics.gcloud.distribution.aggregator.impl;

import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.gcloud.distribution.DistributionBuckets;
import com.nikolaybotev.metrics.gcloud.distribution.aggregator.DistributionAggregator;

import java.io.Serializable;
import java.util.Arrays;

public class DistributionAggregatorSync implements DistributionAggregator {
    private final Buckets bucketsDefinition;

    private final long[] buckets;

    private long numSamples;
    private double mean;
    private double sumOfSquaredDeviation;

    private final Object lock = new Serializable() {};

    public DistributionAggregatorSync(Buckets bucketsDefinition) {
        this.bucketsDefinition = bucketsDefinition;

        this.buckets = new long[bucketsDefinition.bucketCount()];
    }

    @Override
    public void add(long value) {
        synchronized (lock) {
            // Update bucket
            var bucket = bucketsDefinition.bucketForValue(value);
            buckets[bucket] += 1;

            // Update numSamples, mean and M2 using Welford's method for accumulating the sum of squared deviations.
            numSamples = numSamples + 1;
            var delta = value - mean;
            mean = mean + (delta / numSamples);
            sumOfSquaredDeviation = sumOfSquaredDeviation + delta * (value - mean);
        }
    }

    @Override
    public DistributionBuckets getAndClear() {
        synchronized (lock) {
            // Make a copy
            var result = new DistributionBuckets(
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
