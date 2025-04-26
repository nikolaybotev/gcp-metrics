package com.nikolaybotev.metrics.buckets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The main sample distribution used in these tests is as follows:
 *
 * <ul>
 *     <li>Number of buckets: 4
 *     <li>Exponential growth factor: 2
 *     <li>Linear scale: 3
 * </ul>
 *
 * Resulting in the following ranges:
 * <p>
 * <code>
 *     (-INF, 3), [3, 6), [6, 12), [12, 24), [24, 48), [48, +INF)
 * </code>
 * <p>
 * See <a href="https://cloud.google.com/logging/docs/logs-based-metrics/distribution-metrics#bucket_layouts">
 *     Configure distribution metrics - Histogram buckets</a> for more examples.
 */
public class ExponentialBucketsTest {
    @Test
    public void bucketForValue_lessThanPositiveMin_isZero() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(2);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_muchLessThanPositiveMin_isZero() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(1);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_zero_isZero() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(0);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lessThanZeroMin_isZero() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(-10);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_zeroWithOneMin_isZero() {
        var subject = new ExponentialBuckets(4, 2, 1);

        var result = subject.bucketForValue(0);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_lotsLessThanOneMin_isZero() {
        var subject = new ExponentialBuckets(4, 2, 1);

        var result = subject.bucketForValue(-1000);

        assertEquals(0, result);
    }

    @Test
    public void bucketForValue_oneWithOneMin_isOne() {
        var subject = new ExponentialBuckets(4, 2, 1);

        var result = subject.bucketForValue(1);

        assertEquals(1, result);
    }

    @Test
    public void bucketForValue_atLowerBoundary_isOne() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(3);

        assertEquals(1, result);
    }

    @Test
    public void bucketForValue_atMiddleOfFirstBucket_isOne() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(4);

        assertEquals(1, result);
    }

    @Test
    public void bucketForValue_atEndOfFirstBucket_isOne() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(5);

        assertEquals(1, result);
    }

    @Test
    public void bucketForValue_atFirstBoundary_isTwo() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(6);

        assertEquals(2, result);
    }

    @Test
    public void bucketForValue_atSecondBucket_isTwo() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(11);

        assertEquals(2, result);
    }

    @Test
    public void bucketForValue_atSecondBoundary_isThree() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(12);

        assertEquals(3, result);
    }

    @Test
    public void bucketForValue_atThirdBucket_isThree() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(23);

        assertEquals(3, result);
    }

    @Test
    public void bucketForValue_atThirdBoundary_isFour() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(24);

        assertEquals(4, result);
    }

    @Test
    public void bucketForValue_belowUpperBound_isInLastBucket() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(47);

        assertEquals(4, result);
    }

    @Test
    public void bucketForValue_atUpperBound_isInOverflowBucket() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(48);

        assertEquals(5, result);
    }

    @Test
    public void bucketForValue_wayBeyondLastBucket_isFive() {
        var subject = new ExponentialBuckets(4, 2, 3);

        var result = subject.bucketForValue(50_000);

        assertEquals(5, result);
    }
}
