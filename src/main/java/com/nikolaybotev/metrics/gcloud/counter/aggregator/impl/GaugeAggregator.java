package com.nikolaybotev.metrics.gcloud.counter.aggregator.impl;

import com.nikolaybotev.metrics.gcloud.counter.aggregator.CounterAggregator;

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
