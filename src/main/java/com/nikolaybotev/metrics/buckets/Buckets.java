package com.nikolaybotev.metrics.buckets;

import java.io.Serializable;

public sealed interface Buckets extends Serializable permits ExponentialBuckets, LinearBuckets {
    int bucketForValue(long value);

    int numFiniteBuckets();

    /**
     * @return the total number of buckets including finite and overflow buckets
     */
    default int bucketCount() {
        return numFiniteBuckets() + 2;
    }
}
