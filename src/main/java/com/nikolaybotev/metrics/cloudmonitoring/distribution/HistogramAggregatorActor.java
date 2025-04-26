package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nikolaybotev.metrics.buckets.Buckets;

import java.util.Arrays;
import java.util.concurrent.*;

public class HistogramAggregatorActor implements HistogramAggregator {
    private final Buckets bucketsDefinition;

    private final long[] buckets;

    private long numSamples;
    private double mean;
    private double sumOfSquaredDeviation;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

    public HistogramAggregatorActor(Buckets bucketsDefinition) {
        this.bucketsDefinition = bucketsDefinition;

        this.buckets = new long[bucketsDefinition.count() + 2];
    }

    @Override
    public void add(long value) {
        executor.execute(() -> {
            // Update bucket
            var bucket = bucketsDefinition.bucketForValue(value);
            buckets[bucket] += 1;

            // Update count, mean and M2 using Welford's method for accumulating the sum of squared deviations.
            numSamples = numSamples + 1;
            var delta = value - mean;
            mean = mean + (delta / numSamples);
            sumOfSquaredDeviation = sumOfSquaredDeviation + delta * (value - mean);
        });
    }

    @Override
    public HistogramBuckets getAndClear() {
        var future = new CompletableFuture<HistogramBuckets>();// new Future<HistogramBuckets>();
        executor.execute(() -> {
            // Make a copy
            var result = new HistogramBuckets(
                    Arrays.copyOf(buckets, buckets.length), numSamples, mean, sumOfSquaredDeviation);

            // Clear
            Arrays.fill(buckets, 0);
            numSamples = 0;
            mean = 0;
            sumOfSquaredDeviation = 0;

            future.complete(result);
        });
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
