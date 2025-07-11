package com.nikolaybotev.metrics.gcloud.emitter;

import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;

public interface GCloudMetricAggregator {
    TimeSeries getTimeSeriesTemplate();
    TypedValue getAndClear();
}
