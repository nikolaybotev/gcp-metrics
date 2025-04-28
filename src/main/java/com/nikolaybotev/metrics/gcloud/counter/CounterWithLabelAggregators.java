package com.nikolaybotev.metrics.gcloud.counter;

import com.google.common.collect.ImmutableList;
import com.nikolaybotev.metrics.gcloud.counter.aggregator.CounterAggregatorWriter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CounterWithLabelAggregators {
    private final Function<ImmutableList<String>, CounterAggregatorWriter> aggregatorFactory;
    private final ConcurrentMap<ImmutableList<String>, CounterAggregatorWriter> aggregators;

    public CounterWithLabelAggregators(Function<ImmutableList<String>, CounterAggregatorWriter> aggregatorFactory) {
        this.aggregatorFactory = aggregatorFactory;
        this.aggregators = new ConcurrentHashMap<>();
    }

    public CounterAggregatorWriter getAggregatorForLabelValue(ImmutableList<String> labelValue) {
        return aggregators.computeIfAbsent(labelValue, aggregatorFactory);
    }
}
