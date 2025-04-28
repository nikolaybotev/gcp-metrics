package com.nikolaybotev.metrics.noop;

import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

public class NoopMetrics implements Metrics {
    @Override
    public Counter counter(String name, String... label) {
        return (n, labelValue) -> {};
    }

    @Override
    public Gauge gauge(String name, String... label) {
        return (observation, labelValue) -> {};
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets) {
        return value -> {};
    }

    @Override
    public void addEmitListener(SerializableSupplier<Runnable> listener) {}

    @Override
    public void flush() {}
}
