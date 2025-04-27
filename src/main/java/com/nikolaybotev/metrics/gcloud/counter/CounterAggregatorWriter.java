package com.nikolaybotev.metrics.gcloud.counter;

public interface CounterAggregatorWriter {
    void add(long value);
}
