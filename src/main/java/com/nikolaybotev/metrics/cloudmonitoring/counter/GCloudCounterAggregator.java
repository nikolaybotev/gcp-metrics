package com.nikolaybotev.metrics.cloudmonitoring.counter;

import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.nikolaybotev.metrics.cloudmonitoring.emitter.GCloudMetricAggregator;

public class GCloudCounterAggregator implements GCloudMetricAggregator {
    private final TimeSeries timeSeriesTemplate;
    private final CounterAggregator aggregator;

    public GCloudCounterAggregator(TimeSeries timeSeriesTemplate, CounterAggregator aggregator) {
        this.timeSeriesTemplate = timeSeriesTemplate;
        this.aggregator = aggregator;
    }

    @Override
    public TimeSeries getTimeSeriesTemplate() {
        return timeSeriesTemplate;
    }

    @Override
    public TypedValue getAndClear() {
        var count = aggregator.getAndClear();

        return TypedValue.newBuilder()
                .setInt64Value(count)
                .build();
    }
}
