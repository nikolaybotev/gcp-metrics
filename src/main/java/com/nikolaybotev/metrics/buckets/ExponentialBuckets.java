package com.nikolaybotev.metrics.buckets;

public record ExponentialBuckets(int numFiniteBuckets, double growthFactor, double scale) implements Buckets {
    @Override
    public int bucketForValue(long value) {
        if (value <= 0) {
            return 0;
        }
        return (int) log(growthFactor, value / scale) + 1;
    }

    @Override
    public int count() {
        return numFiniteBuckets;
    }

    private static double log(double base, double n) {
        return Math.log(n) / Math.log(base);
    }
}
