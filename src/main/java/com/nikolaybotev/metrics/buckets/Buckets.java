package com.nikolaybotev.metrics.buckets;

import java.io.Serializable;

public interface Buckets extends Serializable {
    int bucketForValue(long value);
    int finiteBucketCount();

    default int bucketCount() {
        return finiteBucketCount() + 2;
    }
}
