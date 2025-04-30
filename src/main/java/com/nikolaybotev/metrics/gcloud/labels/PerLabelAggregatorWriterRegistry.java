package com.nikolaybotev.metrics.gcloud.labels;

import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class PerLabelAggregatorWriterRegistry<T> implements LabelAggregatorWriterRegistry<T> {
    private final int labelCount;
    private final Function<ImmutableList<String>, T> aggregatorFactory;
    private final ConcurrentMap<ImmutableList<String>, T> aggregators;

    public PerLabelAggregatorWriterRegistry(int labelCount, Function<ImmutableList<String>, T> aggregatorFactory) {
        this.labelCount = labelCount;
        this.aggregatorFactory = aggregatorFactory;
        this.aggregators = new ConcurrentHashMap<>();
    }

    @Override
    public T getAggregatorForLabelValue(String... labelValue) {
        var labelValuesForKeys = getLabelValuesForKeys(labelValue);
        return aggregators.computeIfAbsent(labelValuesForKeys, aggregatorFactory);
    }

    private ImmutableList<String> getLabelValuesForKeys(String[] labelValue) {
        if (labelValue.length <= labelCount) {
            return ImmutableList.copyOf(labelValue);
        }

        // Trim the list of values...
        return ImmutableList.copyOf(Arrays.stream(labelValue).limit(labelCount).iterator());
    }
}
