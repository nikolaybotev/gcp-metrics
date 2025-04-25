package com.nikolaybotev.metrics.cloudmonitoring;

import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.cloudmonitoring.counter.LabeledCounterAggregators;
import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableLazy;

import javax.annotation.Nullable;
import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudCounter implements Counter {
    @Serial
    private static final long serialVersionUID = -5905420020271274803L;

    private final GCloudMetrics metrics;

    private final String name;

    private final @Nullable String labelKey;

    private final SerializableLazy<LabeledCounterAggregators> aggregators;

    public GCloudCounter(GCloudMetrics metrics, String name, @Nullable String labelKey,
                         SerializableLazy<LabeledCounterAggregators> aggregators) {
        this.metrics = metrics;
        this.name = name;
        this.labelKey = labelKey;
        this.aggregators = aggregators;
    }

    @Override
    public void inc(long n, @Nullable String labelValue) {
        aggregators.getValue().getAggregatorForLabelValue(labelValue).add(n);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getCounter(name, labelKey);
    }
}
