package com.nikolaybotev.metrics.buckets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinearBucketsTest {
    @Test
    public void bucketForValue_lessThanPositiveMin_isZero() {
        var subject = new LinearBuckets(250, 250, 200);

        var result = subject.bucketForValue(20);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lotsLessThanPositiveMin_isZero() {
        var subject = new LinearBuckets(1000, 250, 200);

        var result = subject.bucketForValue(90);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lessThanZeroMin_isZero() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(-1);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lotsLessThanZeroMin_isZero() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(-1000);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_atLowerBoundary_isOne() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(0);

        assertEquals(1, result);
    }

    @Test
    public void bucketForValue_atFirstBoundary_isTwo() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(250);

        assertEquals(2, result);
    }

    @Test
    public void bucketForValue_atSecondBucket_isTwo() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(421);

        assertEquals(2, result);
    }

    @Test
    public void bucketForValue_atLastBucket_isTwoHundred() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(50_000);

        assertEquals(201, result);
    }

    @Test
    public void bucketForValue_belowUpperBound_isInLastBucket() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(49_999);

        assertEquals(200, result);
    }

    @Test
    public void bucketForValue_lotsHigherThanMax_isTwoHundredOne() {
        var subject = new LinearBuckets(0, 250, 200);

        var result = subject.bucketForValue(880_000);

        assertEquals(201, result);
    }
}
