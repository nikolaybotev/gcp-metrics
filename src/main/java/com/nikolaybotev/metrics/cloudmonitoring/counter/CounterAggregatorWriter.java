package com.nikolaybotev.metrics.cloudmonitoring.counter;

public interface CounterAggregatorWriter {
    void add(long value);
}
