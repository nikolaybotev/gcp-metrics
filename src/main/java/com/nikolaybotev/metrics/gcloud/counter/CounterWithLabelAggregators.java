package com.nikolaybotev.metrics.gcloud.counter;

import com.google.common.collect.ImmutableList;
import com.nikolaybotev.metrics.gcloud.counter.aggregator.CounterAggregatorWriter;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CounterWithLabelAggregators {
    private final int labelCount;
    private final Function<ImmutableList<String>, CounterAggregatorWriter> aggregatorFactory;
    private final ConcurrentMap<ImmutableList<String>, CounterAggregatorWriter> aggregators;

    public CounterWithLabelAggregators(int labelCount, Function<ImmutableList<String>, CounterAggregatorWriter> aggregatorFactory) {
        this.labelCount = labelCount;
        this.aggregatorFactory = aggregatorFactory;
        this.aggregators = new ConcurrentHashMap<>();
    }

    public CounterAggregatorWriter getAggregatorForLabelValue(String... labelValue) {
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
