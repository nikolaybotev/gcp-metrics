package com.nikolaybotev.metrics.buckets;

import java.io.Serializable;

public interface Buckets extends Serializable {
    int bucketForValue(long value);

    int numFiniteBuckets();

    /**
     * @return the total number of buckets including finite and overflow buckets
     */
    default int bucketCount() {
        return numFiniteBuckets() + 2;
    }
}
