package com.nikolaybotev.metrics.cloudmonitoring.distribution;

public interface HistogramReader {
    HistogramBuckets getAndClear();
}
