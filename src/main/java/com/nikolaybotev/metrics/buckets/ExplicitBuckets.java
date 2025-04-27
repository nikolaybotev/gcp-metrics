package com.nikolaybotev.metrics.buckets;

import java.io.Serial;

public record ExplicitBuckets(double ... bounds) implements Buckets {
    @Serial
    private static final long serialVersionUID = -4820802298968551786L;

    @Override
    public int bucketForValue(long value) {
        var i = 0;
        while (i < bounds.length && value >= bounds[i]) {
            i++;
        }
        return i;
    }

    @Override
    public int numFiniteBuckets() {
        return bounds.length - 1;
    }
}
