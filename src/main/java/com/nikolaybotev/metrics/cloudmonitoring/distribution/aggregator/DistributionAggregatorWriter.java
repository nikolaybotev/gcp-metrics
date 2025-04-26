package com.nikolaybotev.metrics.cloudmonitoring.distribution.aggregator;

public interface DistributionAggregatorWriter {
    void add(long value);
}
