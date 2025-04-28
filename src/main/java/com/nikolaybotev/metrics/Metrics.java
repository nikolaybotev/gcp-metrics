package com.nikolaybotev.metrics;

import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.buckets.LinearBuckets;
import com.nikolaybotev.metrics.util.SerializableRunnable;

import java.io.Serializable;

public interface Metrics extends Serializable {
    Counter counter(String name, String... label);

    Gauge gauge(String name, String... label);

    Distribution distribution(String name, String unit, Buckets buckets);

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

    void addEmitListener(SerializableRunnable listener);

    void flush();
}
