package com.nikolaybotev.metrics.buckets;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExplicitBucketsTest {
    @Test
    public void bucketForValue_lessThanPositiveMin_isZero() {
        var subject = new ExplicitBuckets(250, 500, 750, 800);

        var result = subject.bucketForValue(20);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lotsLessThanPositiveMin_isZero() {
        var subject = new ExplicitBuckets(250, 500, 750, 800);

        var result = subject.bucketForValue(90);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lessThanZeroMin_isZero() {
        var subject = new ExplicitBuckets(0, 250, 500, 750, 800);

        var result = subject.bucketForValue(-1);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lotsLessThanZeroMin_isZero() {
        var subject = new ExplicitBuckets(0, 250, 500, 750, 800);

        var result = subject.bucketForValue(-1000);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_atLowerBoundary_isOne() {
        var subject = new ExplicitBuckets(0, 250, 300);

        var result = subject.bucketForValue(0);

        assertEquals(1, result);
    }

    @Test
    public void bucketForValue_atFirstBoundary_isTwo() {
        var subject = new ExplicitBuckets(0, 250, 500, 750, 800);


        var result = subject.bucketForValue(250);

        assertEquals(2, result);
    }

    @Test
    public void bucketForValue_atSecondBucket_isTwo() {
        var subject = new LinearBuckets(0, 250, 500);

        var result = subject.bucketForValue(421);

        assertEquals(2, result);
    }

    @Test
    public void bucketForValue_atLastBucket_isTwoHundred() {
        var subject = new ExplicitBuckets(0, 250, 500);

        var result = subject.bucketForValue(50_000);

        assertEquals(3, result);
    }

    @Test
    public void bucketForValue_belowUpperBound_isInLastBucket() {
        var subject = new ExplicitBuckets(0, 250, 500, 740);

        var result = subject.bucketForValue(49_999);

        assertEquals(4, result);
    }

    @Test
    public void bucketForValue_lotsHigherThanMax_isTwoHundredOne() {
        var bounds = new double[201];
        Arrays.setAll(bounds, n -> n * 250);
        var subject = new ExplicitBuckets(bounds);

        var result = subject.bucketForValue(880_000);

        assertEquals(201, result);
    }
}
