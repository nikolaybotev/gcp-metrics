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
            // Google Cloud Monitoring dashboard do not correctly display the overflow buckets in histogram charts when
            // the Linear Buckets specification is used with a distribution time series!
            // Also, the offset for linear buckets is not used as a "start" value for the range, but rather as an offset
            // that is *subtracted* from all the point values and ranges in the histogram. This is surprising and
            // unexpected behavior.
            // Therefore, we expand linear buckets into explicit buckets for the time series so that the data can
            // be displayed correctly on Google Cloud dashboards.
            case LinearBuckets linearBuckets -> from(linearBuckets.toExplicitBuckets());
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
