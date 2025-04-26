package com.nikolaybotev.metrics.cloudmonitoring.distribution;

public interface HistogramWriter {
    void add(long value);
}
