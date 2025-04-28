package com.nikolaybotev.metrics.gcloud.labels;

import com.google.common.collect.ImmutableList;
import com.nikolaybotev.metrics.util.lazy.SerializableLazySync;

import java.util.function.Function;

public interface LabelAggregatorWriterRegistry<T> {
    static <T> LabelAggregatorWriterRegistry<T> create(int labelCount, Function<ImmutableList<String>, T> aggregatorFactory) {
        return switch (labelCount) {
            case 0 -> new SingleAggregatorWriterRegistry<>(new SerializableLazySync<>(() -> aggregatorFactory.apply(ImmutableList.of())));
            default -> new PerLabelAggregatorWriterRegistry<>(labelCount, aggregatorFactory);
        };
    }

    T getAggregatorForLabelValue(String... labelValue);
}
