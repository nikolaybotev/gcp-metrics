package com.nikolaybotev.metrics.buckets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinearBucketsTest {
    @Test
    public void toExplicitBuckets_withSimpleInput_producesCorrectResult() {
        var subject = new LinearBuckets(0, 10, 3);

        var result = subject.toExplicitBuckets();

        assertArrayEquals(new double[] {0, 10, 20, 30}, result.bounds());
    }

    @Test
    public void toExplicitBuckets_withNonZeroOffset_producesCorrectResult() {
        var subject = new LinearBuckets(20, 12, 4);

        var result = subject.toExplicitBuckets();

        assertArrayEquals(new double[] {20, 32, 44, 56, 68}, result.bounds());
    }

    @Test
    public void toExplicitBuckets_withOneFiniteBucket_producesCorrectResult() {
        var subject = new LinearBuckets(100, 200, 1);

        var result = subject.toExplicitBuckets();

        assertArrayEquals(new double[] {100, 300}, result.bounds());
    }

    @Test
    public void toExplicitBuckets_withOneFiniteBucketSize1_producesCorrectResult() {
        var subject = new LinearBuckets(1, 1, 1);

        var result = subject.toExplicitBuckets();

        assertArrayEquals(new double[] {1, 2}, result.bounds());
    }

    @Test
    public void bucketForValue_with1FiniteSmallestBucket_isCorrect() {
        var subject = new LinearBuckets(1, 1, 1);

        assertEquals(0, subject.bucketForValue(-1));
        assertEquals(0, subject.bucketForValue(0));
        assertEquals(1, subject.bucketForValue(1));
        assertEquals(2, subject.bucketForValue(2));
        assertEquals(2, subject.bucketForValue(300));
    }

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
