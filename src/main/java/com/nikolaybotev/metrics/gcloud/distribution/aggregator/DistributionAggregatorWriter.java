package com.nikolaybotev.metrics.gcloud.distribution.aggregator;

public interface DistributionAggregatorWriter {
    void add(long value);
}
