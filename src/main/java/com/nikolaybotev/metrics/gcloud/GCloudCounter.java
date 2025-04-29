package com.nikolaybotev.metrics.gcloud;

import com.google.common.collect.ImmutableList;
import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.gcloud.counter.aggregator.CounterAggregatorWriter;
import com.nikolaybotev.metrics.gcloud.labels.LabelAggregatorWriterRegistry;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudCounter implements Counter {
    @Serial
    private static final long serialVersionUID = -1075349040232098340L;

    private final GCloudMetrics metrics;
    private final String name;
    private final String unit;
    private final ImmutableList<String> labelKey;
    private final SerializableLazy<? extends LabelAggregatorWriterRegistry<? extends CounterAggregatorWriter>> aggregators;

    public GCloudCounter(GCloudMetrics metrics, String name, String unit, ImmutableList<String> labelKey,
                         SerializableLazy<? extends LabelAggregatorWriterRegistry<? extends CounterAggregatorWriter>> aggregators) {
        this.metrics = metrics;
        this.name = name;
        this.unit = unit;
        this.labelKey = labelKey;
        this.aggregators = aggregators;
    }

    @Override
    public void inc(long n, String... labelValue) {
        aggregators.getValue().getAggregatorForLabelValue(labelValue).add(n);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getCounter(name, unit, labelKey);
    }
}
