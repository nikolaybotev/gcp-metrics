package com.nikolaybotev.metrics.cloudmonitoring.counter.impl;

import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterAggregator;

import java.util.concurrent.atomic.AtomicLong;

public class CounterAggregatorAtomic implements CounterAggregator {
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
