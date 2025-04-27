package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.google.api.Distribution;
import com.google.common.primitives.Doubles;
import com.nikolaybotev.metrics.buckets.Buckets;

public final class GCloudBucketOptions {
    public static Distribution.BucketOptions from(Buckets buckets) {
        // Google Cloud Monitoring dashboard do not correctly display the overflow buckets in histogram charts when
        // the Linear Buckets specification is used with a distribution time series!
        //
        // Also, the offset for linear buckets is not used as a "start" value for the range, but rather as an offset
        // that is *subtracted* from all the point values and ranges in the histogram. This is surprising and
        // unexpected behavior.
        //
        // Therefore, we expand linear and exponential bucket definitions into explicit buckets for the time series so
        // that the data can be displayed correctly on Google Cloud dashboards.
        var bounds = buckets.toExplicitBuckets().bounds();
        return Distribution.BucketOptions.newBuilder()
                .setExplicitBuckets(Distribution.BucketOptions.Explicit.newBuilder()
                        .addAllBounds(Doubles.asList(bounds)))
                .build();
    }

    private GCloudBucketOptions() {}
}
