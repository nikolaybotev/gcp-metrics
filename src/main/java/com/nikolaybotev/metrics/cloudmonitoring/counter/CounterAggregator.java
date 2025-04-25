package com.nikolaybotev.metrics.cloudmonitoring.counter;

import java.util.concurrent.atomic.AtomicLong;

public class CounterAggregator {
    private final AtomicLong counter = new AtomicLong();

    public void add(long value) {
        counter.addAndGet(value);
    }

    public long getCurrentValue() {
        return counter.get();
    }
}
