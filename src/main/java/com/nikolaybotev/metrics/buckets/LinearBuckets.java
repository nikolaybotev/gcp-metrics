package com.nikolaybotev.metrics.buckets;

import java.io.Serial;

public record LinearBuckets(long offset, long step, int finiteBucketCount) implements Buckets {
    @Serial
    private static final long serialVersionUID = -4022957706568722400L;

    @Override
    public int bucketForValue(long value) {
        return (int) Math.min(Math.max(0, (value - offset + step) / step), finiteBucketCount + 1);
    }
}
