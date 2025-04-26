package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nikolaybotev.metrics.buckets.Buckets;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedDeque;

public class HistogramAggregatorMem implements HistogramAggregator {
    private final Buckets bucketsDefinition;

    private final long[] buckets;

    private long numSamples;
    private double mean;
    private double sumOfSquaredDeviation;

    private final Object lock = new Serializable() {};
    private final ConcurrentLinkedDeque<Long> samples = new ConcurrentLinkedDeque<>();

    public HistogramAggregatorMem(Buckets bucketsDefinition) {
        this.bucketsDefinition = bucketsDefinition;

        this.buckets = new long[bucketsDefinition.count() + 2];
        Thread sampleProcessor = new ThreadFactoryBuilder().setDaemon(true).build().newThread(() -> {
            //noinspection InfiniteLoopStatement
            do {
                Long sample;
                while ((sample = samples.pollFirst()) != null) {
                    doAdd(sample);
                }
                Thread.yield();
            } while (true);
        });
        sampleProcessor.start();
    }

    @Override
    public void add(long value) {
        samples.add(value);
    }

    private void doAdd(long value) {
        synchronized (lock) {
            // Update bucket
            var bucket = bucketsDefinition.bucketForValue(value);
            buckets[bucket] += 1;

            // Update count, mean and M2 using Welford's method for accumulating the sum of squared deviations.
            numSamples = numSamples + 1;
            var delta = value - mean;
            mean = mean + (delta / numSamples);
            sumOfSquaredDeviation = sumOfSquaredDeviation + delta * (value - mean);
        }
    }

    @Override
    public HistogramBuckets getAndClear() {
        synchronized (lock) {
            // Make a copy
            var result = new HistogramBuckets(
                    Arrays.copyOf(buckets, buckets.length), numSamples, mean, sumOfSquaredDeviation);

            // Clear
            Arrays.fill(buckets, 0);
            numSamples = 0;
            mean = 0;
            sumOfSquaredDeviation = 0;

            return result;
        }
    }
}
