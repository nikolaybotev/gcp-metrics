package com.nikolaybotev.metrics.gcloud.counter;

import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.nikolaybotev.metrics.gcloud.emitter.GCloudMetricAggregator;

public class GCloudCounterAggregator implements GCloudMetricAggregator {
    private final TimeSeries timeSeriesTemplate;
    private final CounterAggregatorReader aggregator;

    public GCloudCounterAggregator(TimeSeries timeSeriesTemplate, CounterAggregatorReader aggregator) {
        this.timeSeriesTemplate = timeSeriesTemplate;
        this.aggregator = aggregator;
    }

    @Override
    public TimeSeries getTimeSeriesTemplate() {
        return timeSeriesTemplate;
    }

    @Override
    public TypedValue getAndClear() {
        var value = aggregator.getCurrentValue();

        return TypedValue.newBuilder()
                .setInt64Value(value)
                .build();
    }
}
