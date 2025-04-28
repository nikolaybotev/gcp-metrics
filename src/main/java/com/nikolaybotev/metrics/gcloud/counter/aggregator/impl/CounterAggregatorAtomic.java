package com.nikolaybotev.metrics.gcloud.counter.aggregator.impl;

import com.nikolaybotev.metrics.gcloud.counter.aggregator.CounterAggregator;

import java.util.concurrent.atomic.AtomicLong;

class CounterAggregatorAtomic implements CounterAggregator {
    private final AtomicLong counter = new AtomicLong();

    @Override
    public void add(long value) {
        counter.addAndGet(value);
    }

    @Override
    public long getCurrentValue() {
        return counter.get();
    }
}
