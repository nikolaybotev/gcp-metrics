package com.nikolaybotev.metrics.buckets;

import java.io.Serializable;

public interface Buckets extends Serializable {
    int bucketForValue(long value);
    int count();
}
