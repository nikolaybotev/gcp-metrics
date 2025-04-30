package com.nikolaybotev.metrics.gcloud.labels;

import com.nikolaybotev.metrics.util.lazy.SerializableLazy;

public final class SingleAggregatorWriterRegistry<T> implements LabelAggregatorWriterRegistry<T> {
    private final SerializableLazy<? extends T> aggregator;

    public SingleAggregatorWriterRegistry(SerializableLazy<? extends T> aggregator) {
        this.aggregator = aggregator;
    }

    @Override
    public T getAggregatorForLabelValue(String... labelValue) {
        return aggregator.getValue();
    }
}
