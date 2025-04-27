package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.google.api.Distribution;
import com.google.common.primitives.Doubles;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.buckets.ExplicitBuckets;
import com.nikolaybotev.metrics.buckets.ExponentialBuckets;
import com.nikolaybotev.metrics.buckets.LinearBuckets;

public final class GCloudBucketOptions {
    public static Distribution.BucketOptions from(Buckets buckets) {
        return switch (buckets) {
            case LinearBuckets(double start, double step, int count) ->
                    Distribution.BucketOptions.newBuilder()
                            .setLinearBuckets(Distribution.BucketOptions.Linear.newBuilder()
                                    .setNumFiniteBuckets(count)
                                    .setWidth(step)
                                    .setOffset(start)
                                    .build())
                            .build();
            case ExponentialBuckets(int numFiniteBuckets, double growthFactor, double scale) ->
                    Distribution.BucketOptions.newBuilder()
                            .setExponentialBuckets(Distribution.BucketOptions.Exponential.newBuilder()
                                    .setNumFiniteBuckets(numFiniteBuckets)
                                    .setGrowthFactor(growthFactor)
                                    .setScale(scale)
                                    .build())
                            .build();
            case ExplicitBuckets(double[] bounds) ->
                Distribution.BucketOptions.newBuilder()
                        .setExplicitBuckets(Distribution.BucketOptions.Explicit.newBuilder()
                                .addAllBounds(Doubles.asList(bounds)))
                        .build();
        };
    }

    private GCloudBucketOptions() {}
}
