package com.nikolaybotev.metrics.cloudmonitoring;

import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterAggregatorWriter;
import com.nikolaybotev.metrics.cloudmonitoring.util.lazy.SerializableLazy;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudCounter implements Counter {
    @Serial
    private static final long serialVersionUID = -5905420020271274803L;

    private final GCloudMetrics metrics;

    private final String name;

    private final SerializableLazy<? extends CounterAggregatorWriter> aggregator;

    public GCloudCounter(GCloudMetrics metrics, String name, SerializableLazy<? extends CounterAggregatorWriter> aggregator) {
        this.metrics = metrics;
        this.name = name;
        this.aggregator = aggregator;
    }

    @Override
    public void inc(long n) {
        aggregator.getValue().add(n);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getCounter(name);
    }
}
