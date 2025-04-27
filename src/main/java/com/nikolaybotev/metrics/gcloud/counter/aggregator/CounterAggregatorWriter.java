package com.nikolaybotev.metrics.gcloud.counter.aggregator;

public interface CounterAggregatorWriter {
    void add(long value);
}
