package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import java.util.Arrays;

public record DistributionBuckets(
        long[] buckets,
        long numSamples,
        double mean,
        double sumOfSquaredDeviation) {
    public static DistributionBuckets merge(DistributionBuckets a, DistributionBuckets b) {
        var numBuckets = a.buckets().length;
        assert b.buckets().length == numBuckets;

        var buckets = new long[numBuckets];
        Arrays.setAll(buckets, n -> a.buckets()[n] + b.buckets()[n]);

        var numSamples = a.numSamples() + b.numSamples();
        var mean = (a.numSamples() * a.mean() + b.numSamples() * b.mean()) / numSamples;
        var sumSquaredDeviations = a.sumOfSquaredDeviation() + b.sumOfSquaredDeviation() +
                a.numSamples() * (a.mean() - mean) * (a.mean() - mean) +
                b.numSamples() * (b.mean() - mean) * (b.mean() - mean);

        return new DistributionBuckets(buckets, numSamples, mean, sumSquaredDeviations);
    }
}
