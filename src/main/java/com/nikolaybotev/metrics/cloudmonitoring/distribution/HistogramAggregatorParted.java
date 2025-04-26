package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.nikolaybotev.metrics.buckets.Buckets;

import java.util.Arrays;

public class HistogramAggregatorParted implements HistogramAggregator {
    private static final int bins = Runtime.getRuntime().availableProcessors();
    private static final ThreadLocal<Integer> bin = ThreadLocal.withInitial(() -> Thread.currentThread().hashCode() % bins);

    private final HistogramAggregator[] workers = new HistogramAggregator[bins];
    private final HistogramBuckets identityBucket;

    public HistogramAggregatorParted(Buckets bucketsDefinition) {
        Arrays.setAll(workers, n -> new HistogramAggregatorSync(bucketsDefinition));
        identityBucket = new HistogramBuckets(new long[bucketsDefinition.bucketCount()], 0, 0, 0);
    }

    @Override
    public void add(long value) {
        workers[bin.get()].add(value);
    }

    @Override
    public HistogramBuckets getAndClear() {
        return Arrays.stream(workers)
                .map(HistogramReader::getAndClear)
                .filter(histogramBuckets -> histogramBuckets.numSamples() > 0)
                .reduce(identityBucket, HistogramAggregatorParted::mergeBuckets);
    }

    private static HistogramBuckets mergeBuckets(HistogramBuckets a, HistogramBuckets b) {
        var numBuckets = a.buckets().length;
        assert b.buckets().length == numBuckets;

        var buckets = new long[numBuckets];
        Arrays.setAll(buckets, n -> a.buckets()[n] + b.buckets()[n]);

        var numSamples = a.numSamples() + b.numSamples();
        var mean = (a.numSamples() * a.mean() + b.numSamples() * b.mean()) / numSamples;
        var sumSquaredDeviations = a.sumOfSquaredDeviation() + b.sumOfSquaredDeviation() +
                a.numSamples() * (a.mean() - mean) * (a.mean() - mean) +
                b.numSamples() * (b.mean() - mean) * (b.mean() - mean);

        return new HistogramBuckets(buckets, numSamples, mean, sumSquaredDeviations);
    }
}
