package com.nikolaybotev.metrics.gcloud.distribution.aggregator.impl;

import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.gcloud.distribution.DistributionBuckets;
import com.nikolaybotev.metrics.gcloud.distribution.aggregator.DistributionAggregator;
import com.nikolaybotev.metrics.gcloud.distribution.aggregator.DistributionAggregatorReader;

import java.util.Arrays;

public class DistributionAggregatorParted implements DistributionAggregator {
    private static final int bins = Runtime.getRuntime().availableProcessors() * 4;
    private static final ThreadLocal<Integer> bin = ThreadLocal.withInitial(() -> Thread.currentThread().hashCode() % bins);

    private final DistributionAggregator[] workers = new DistributionAggregator[bins];
    private final DistributionBuckets identityBucket;

    public DistributionAggregatorParted(Buckets bucketsDefinition) {
        Arrays.setAll(workers, n -> new DistributionAggregatorSync(bucketsDefinition));
        identityBucket = new DistributionBuckets(new long[bucketsDefinition.bucketCount()], 0, 0, 0);
    }

    @Override
    public void add(long value) {
        workers[bin.get()].add(value);
    }

    @Override
    public DistributionBuckets getAndClear() {
        return Arrays.stream(workers)
                .map(DistributionAggregatorReader::getAndClear)
                .filter(histogramBuckets -> histogramBuckets.numSamples() > 0)
                .reduce(identityBucket, DistributionBuckets::merge);
    }
}
