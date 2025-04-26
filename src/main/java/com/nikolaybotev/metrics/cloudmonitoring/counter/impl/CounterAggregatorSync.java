package com.nikolaybotev.metrics.cloudmonitoring.counter.impl;

import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterAggregator;

import java.io.Serializable;

public class CounterAggregatorSync implements CounterAggregator {
    private final Object lock = new Serializable() {};

    private long counter;

    @Override
    public void add(long value) {
        synchronized (lock) {
            counter += value;
        }
    }

    @Override
    public long getCurrentValue() {
        return counter;
    }
}
