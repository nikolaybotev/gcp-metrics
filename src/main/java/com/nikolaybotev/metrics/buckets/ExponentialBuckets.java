package com.nikolaybotev.metrics.buckets;

public record ExponentialBuckets(int numFiniteBuckets, double growthFactor, double scale) implements Buckets {
    @Override
    public int bucketForValue(long value) {
        if (value < scale) {
            return 0;
        }
        return Math.min((int) log(growthFactor, value / scale), numFiniteBuckets) + 1;
    }

    @Override
    public int finiteBucketCount() {
        return numFiniteBuckets;
    }

    private static double log(double base, double n) {
        return Math.log(n) / Math.log(base);
    }
}
