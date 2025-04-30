package com.nikolaybotev.metrics.noop;

import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

import java.io.Serial;

public class NoopMetrics implements Metrics {
    @Serial
    private static final long serialVersionUID = 2053206102482869762L;

    @Override
    public Counter counter(String name, String unit, String... label) {
        return new NoopCounter();
    }

    @Override
    public Gauge gauge(String name, String unit, String... label) {
        return new NoopGauge();
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets, String... label) {
        return new NoopDistribution();
    }

    @Override
    public void addEmitListener(SerializableSupplier<Runnable> listener) {}

    @Override
    public void flush() {}

    private static class NoopCounter implements Counter {
        @Serial
        private static final long serialVersionUID = -22477207774271852L;

        @Override
        public void inc(long n, String... labelValue) {}
    }

    private static class NoopGauge implements Gauge {
        @Serial
        private static final long serialVersionUID = 8531719978727983683L;

        @Override
        public void emit(long observation, String... labelValue) {}
    }

    private static class NoopDistribution implements Distribution {
        @Serial
        private static final long serialVersionUID = 7080781924877500968L;

        @Override
        public void update(long value, String... labelValue) {

        }
    }
}
