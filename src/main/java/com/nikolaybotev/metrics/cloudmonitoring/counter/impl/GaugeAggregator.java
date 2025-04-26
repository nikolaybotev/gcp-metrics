package com.nikolaybotev.metrics.cloudmonitoring.counter.impl;

import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterAggregator;

public class GaugeAggregator implements CounterAggregator {
    private volatile long value;

    @Override
    public void add(long value) {
        this.value = value;
    }

    @Override
    public long getCurrentValue() {
        return value;
    }
}
