package com.nikolaybotev.metrics.cloudmonitoring.counter.impl;

import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterAggregator;

import java.util.Arrays;

public class CounterAggregatorParted implements CounterAggregator {
    private static final int bins = Runtime.getRuntime().availableProcessors() * 4;
    private static final ThreadLocal<Integer> bin = ThreadLocal.withInitial(() -> Thread.currentThread().hashCode() % bins);

    private final CounterAggregator[] workers = new CounterAggregator[bins];

    public CounterAggregatorParted() {
        Arrays.setAll(workers, n -> new CounterAggregatorAtomic());
    }

    @Override
    public void add(long value) {
        workers[bin.get()].add(value);
    }

    @Override
    public long getCurrentValue() {
        return  Arrays.stream(workers)
                .mapToLong(CounterAggregator::getCurrentValue)
                .sum();
    }
}
