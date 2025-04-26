package com.nikolaybotev.metrics.cloudmonitoring.distribution;

import com.nikolaybotev.metrics.buckets.Buckets;

import java.util.Arrays;

public class HistogramAggregatorParted implements HistogramAggregator {
    private static final int bins = Runtime.getRuntime().availableProcessors() * 4;
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
                .reduce(identityBucket, HistogramBuckets::merge);
    }
}
