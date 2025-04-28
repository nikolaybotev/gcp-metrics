package com.nikolaybotev.metrics.prefixed;

import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

public class PrefixedMetrics implements Metrics {
    private final Metrics metrics;
    private final String prefix;

    public PrefixedMetrics(Metrics metrics, String prefix) {
        this.metrics = metrics;
        this.prefix = prefix;
    }

    @Override
    public Counter counter(String name, String... label) {
        return metrics.counter(prefix + name, label);
    }

    @Override
    public Gauge gauge(String name, String... label) {
        return metrics.gauge(prefix + name, label);
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets) {
        return metrics.distribution(prefix + name, unit, buckets);
    }

    @Override
    public void addEmitListener(SerializableSupplier<Runnable> listener) {
        metrics.addEmitListener(listener);
    }

    @Override
    public void flush() {
        metrics.flush();
    }
}
