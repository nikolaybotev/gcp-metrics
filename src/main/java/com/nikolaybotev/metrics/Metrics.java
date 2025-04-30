package com.nikolaybotev.metrics;

import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.buckets.LinearBuckets;
import com.nikolaybotev.metrics.labeled.LabeledMetrics;
import com.nikolaybotev.metrics.prefixed.PrefixedMetrics;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

import java.io.Serializable;

/**
 * Metrics top-level API.
 * <p>
 * <h3>Units</h3>
 * See <a href="https://cloud.google.com/monitoring/api/ref_v3/rest/v3/projects.metricDescriptors#MetricDescriptor">
 *     MetricDescriptor</a> for a list of supported metric units.
 */
public interface Metrics extends Serializable {
    default Metrics withPrefix(String prefix) {
        return new PrefixedMetrics(this, prefix);
    }

    default Metrics withLabel(String label, String value) {
        return new LabeledMetrics(this, label, value);
    }

    Counter counter(String name, String unit, String... label);

    Gauge gauge(String name, String unit, String... label);

    Distribution distribution(String name, String unit, Buckets buckets, String... label);

    default Counter counter(String name) {
        return counter(name, "");
    }

    default Gauge gauge(String name) {
        return gauge(name, "");
    }

    default Distribution distribution(String name, Buckets buckets, String... label) {
        return distribution(name, "", buckets, label);
    }

    default Distribution distribution(String name, String unit, long step, int count, String... label) {
        return distribution(name, unit, new LinearBuckets(0, step, count), label);
    }

    default Distribution distribution(String name, long step, int count, String... label) {
        return distribution(name, "", new LinearBuckets(0, step, count), label);
    }

    default Distribution distribution(String name, String... label) {
        return distribution(name, "", new LinearBuckets(0, 100, 100), label);
    }

    void addEmitListener(SerializableSupplier<Runnable> listener);

    void flush();
}
