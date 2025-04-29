package com.nikolaybotev.metrics;

import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.buckets.LinearBuckets;
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
    Counter counter(String name, String unit, String... label);

    Gauge gauge(String name, String unit, String... label);

    Distribution distribution(String name, String unit, Buckets buckets);

    default Counter counter(String name) {
        return counter(name, "");
    }

    default Gauge gauge(String name) {
        return gauge(name, "");
    }

    default Distribution distribution(String name, Buckets buckets) {
        return distribution(name, "", buckets);
    }

    default Distribution distribution(String name, String unit, long step, int count) {
        return distribution(name, unit, new LinearBuckets(0, step, count));
    }

    default Distribution distribution(String name, long step, int count) {
        return distribution(name, "", new LinearBuckets(0, step, count));
    }

    default Distribution distribution(String name) {
        return distribution(name, "", new LinearBuckets(0, 100, 100));
    }

    void addEmitListener(SerializableSupplier<Runnable> listener);

    void flush();
}
