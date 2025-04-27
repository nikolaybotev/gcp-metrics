package com.nikolaybotev.metrics.cloudmonitoring;

import com.nikolaybotev.metrics.GaugeWithLabel;
import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterWithLabelAggregators;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudGaugeWithLabel implements GaugeWithLabel {
    @Serial
    private static final long serialVersionUID = -788325344231105755L;

    private final GCloudMetrics metrics;

    private final String name;

    private final String labelKey;

    private final SerializableLazy<CounterWithLabelAggregators> aggregators;

    public GCloudGaugeWithLabel(GCloudMetrics metrics, String name, String labelKey,
                                SerializableLazy<CounterWithLabelAggregators> aggregators) {
        this.metrics = metrics;
        this.name = name;
        this.labelKey = labelKey;
        this.aggregators = aggregators;
    }

    @Override
    public void emit(String labelValue, long observation) {
        aggregators.getValue().getAggregatorForLabelValue(labelValue).add(observation);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getGaugeWithLabel(name, labelKey);
    }
}
