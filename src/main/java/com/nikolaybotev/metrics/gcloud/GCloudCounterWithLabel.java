package com.nikolaybotev.metrics.gcloud;

import com.nikolaybotev.metrics.CounterWithLabel;
import com.nikolaybotev.metrics.gcloud.counter.CounterWithLabelAggregators;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudCounterWithLabel implements CounterWithLabel {
    @Serial
    private static final long serialVersionUID = -1075349040232098340L;

    private final GCloudMetrics metrics;

    private final String name;

    private final String labelKey;

    private final SerializableLazy<CounterWithLabelAggregators> aggregators;

    public GCloudCounterWithLabel(GCloudMetrics metrics, String name, String labelKey,
                                  SerializableLazy<CounterWithLabelAggregators> aggregators) {
        this.metrics = metrics;
        this.name = name;
        this.labelKey = labelKey;
        this.aggregators = aggregators;
    }

    @Override
    public void inc(String labelValue, long n) {
        aggregators.getValue().getAggregatorForLabelValue(labelValue).add(n);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getCounterWithLabel(name, labelKey);
    }
}
