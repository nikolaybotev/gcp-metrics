package com.nikolaybotev.metrics.labeled;

import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

import java.io.Serial;

public class LabeledMetrics implements Metrics {
    @Serial
    private static final long serialVersionUID = -3703253347261821872L;

    private final Metrics metrics;
    private final String label;
    private final String value;

    public LabeledMetrics(Metrics metrics, String label, String value) {
        this.metrics = metrics;
        this.label = label;
        this.value = value;
    }

    @Override
    public Counter counter(String name, String unit, String... labels) {
        return new LabeledCounter(metrics.counter(name, unit, merge(label, labels)));
    }

    @Override
    public Gauge gauge(String name, String unit, String... labels) {
        return new LabeledGauge(metrics.gauge(name, unit, merge(label, labels)));
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets, String... labels) {
        return new LabeledDistribution(metrics.distribution(name, unit, buckets, merge(label, labels)));
    }

    @Override
    public void addEmitListener(SerializableSupplier<Runnable> listener) {
        metrics.addEmitListener(listener);
    }

    @Override
    public void flush() {
        metrics.flush();
    }

    private class LabeledCounter implements Counter {
        @Serial
        private static final long serialVersionUID = -1932932160362036729L;

        private final Counter counter;

        LabeledCounter(Counter counter) {
            this.counter = counter;
        }

        @Override
        public void inc(long n, String... labelValues) {
            counter.inc(n, merge(value, labelValues));
        }
    }

    private class LabeledGauge implements Gauge {
        @Serial
        private static final long serialVersionUID = 8576081810291745524L;

        private final Gauge gauge;

        LabeledGauge(Gauge counter) {
            this.gauge = counter;
        }

        @Override
        public void emit(long observation, String... labelValues) {
            gauge.emit(observation, merge(value, labelValues));
        }
    }

    private class LabeledDistribution implements Distribution {
        @Serial
        private static final long serialVersionUID = -7648207379360594657L;

        private final Distribution distribution;

        LabeledDistribution(Distribution distribution) {
            this.distribution = distribution;
        }

        @Override
        public void update(long val, String... labelValues) {
            distribution.update(val, merge(value, labelValues));
        }
    }

    private static String[] merge(String value, String... rest) {
        var result = new String[rest.length + 1];
        result[0] = value;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }
}
