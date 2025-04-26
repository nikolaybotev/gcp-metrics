package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.google.api.Distribution;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.buckets.LinearBuckets;

public final class GCloudBucketOptions {
    public static Distribution.BucketOptions from(Buckets buckets) {
        if (buckets instanceof LinearBuckets(long start, long step, int count)) {
            return Distribution.BucketOptions.newBuilder()
                    .setLinearBuckets(Distribution.BucketOptions.Linear.newBuilder()
                            .setNumFiniteBuckets(count)
                            .setWidth(step)
                            .setOffset(start)
                            .build())
                    .build();
        }
        throw new IllegalArgumentException("Unexpected value: " + buckets);
    }

    private GCloudBucketOptions() {}
}
