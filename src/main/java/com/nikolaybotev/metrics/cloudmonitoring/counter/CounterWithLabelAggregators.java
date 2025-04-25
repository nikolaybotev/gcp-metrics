package com.nikolaybotev.metrics.cloudmonitoring.counter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CounterWithLabelAggregators {
    private final Function<String, CounterAggregator> aggregatorFactory;
    private final ConcurrentMap<String, CounterAggregator> aggregators;

    public CounterWithLabelAggregators(Function<String, CounterAggregator> aggregatorFactory) {
        this.aggregatorFactory = aggregatorFactory;
        this.aggregators = new ConcurrentHashMap<>();
    }

    public CounterAggregator getAggregatorForLabelValue(String labelValue) {
        return aggregators.computeIfAbsent(labelValue, aggregatorFactory);
    }
}
