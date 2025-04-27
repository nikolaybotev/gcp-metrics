package com.nikolaybotev.metrics.gcloud.counter.impl;

import com.nikolaybotev.metrics.gcloud.counter.CounterAggregator;

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
