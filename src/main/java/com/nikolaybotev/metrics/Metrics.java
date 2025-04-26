package com.nikolaybotev.metrics;

import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.buckets.LinearBuckets;

import java.io.Serializable;

public interface Metrics extends Serializable {
    Counter counter(String name);
    CounterWithLabel counterWithLabel(String name, String label);

    Gauge gauge(String name);
    GaugeWithLabel gaugeWithLabel(String name, String label);

    Distribution distribution(String name, String unit, Buckets buckets);

    default Distribution distribution(String name, String unit, long start, long step, int count) {
        return distribution(name, unit, new LinearBuckets(start, step, count));
    }

    default Distribution distribution(String name, long start, long step, int count) {
        return distribution(name, "", start, step, count);
    }

    default Distribution distribution(String name, long step, int count) {
        return distribution(name, "", 0, step, count);
    }
}
