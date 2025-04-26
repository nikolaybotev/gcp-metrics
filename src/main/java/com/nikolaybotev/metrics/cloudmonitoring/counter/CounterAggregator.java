package com.nikolaybotev.metrics.cloudmonitoring.counter;

import java.util.concurrent.atomic.AtomicLong;

public class CounterAggregator implements CounterAggregatorWriter, CounterAggregatorReader {
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
