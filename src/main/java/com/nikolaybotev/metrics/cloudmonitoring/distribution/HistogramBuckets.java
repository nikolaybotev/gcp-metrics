package com.nikolaybotev.metrics.cloudmonitoring.distribution;

public record HistogramBuckets(
        long[] buckets,
        long numSamples,
        double mean,
        double sumOfSquaredDeviation
) {}
