package com.nikolaybotev.metrics.prefixed;

import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

import java.io.Serial;

public class PrefixedMetrics implements Metrics {
    @Serial
    private static final long serialVersionUID = 2917232774129942434L;

    private final Metrics metrics;
    private final String prefix;

    public PrefixedMetrics(Metrics metrics, String prefix) {
        this.metrics = metrics;
        this.prefix = prefix;
    }

    @Override
    public Counter counter(String name, String unit, String... label) {
        return metrics.counter(prefix + name, unit, label);
    }

    @Override
    public Gauge gauge(String name, String unit, String... label) {
        return metrics.gauge(prefix + name, unit, label);
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets, String... label) {
        return metrics.distribution(prefix + name, unit, buckets, label);
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
