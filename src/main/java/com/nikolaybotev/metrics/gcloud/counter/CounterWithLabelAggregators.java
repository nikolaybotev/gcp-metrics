package com.nikolaybotev.metrics.gcloud.counter;

import com.nikolaybotev.metrics.gcloud.counter.aggregator.CounterAggregatorWriter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class CounterWithLabelAggregators {
    private final Function<String, CounterAggregatorWriter> aggregatorFactory;
    private final ConcurrentMap<String, CounterAggregatorWriter> aggregators;

    public CounterWithLabelAggregators(Function<String, CounterAggregatorWriter> aggregatorFactory) {
        this.aggregatorFactory = aggregatorFactory;
        this.aggregators = new ConcurrentHashMap<>();
    }

    public CounterAggregatorWriter getAggregatorForLabelValue(String labelValue) {
        return aggregators.computeIfAbsent(labelValue, aggregatorFactory);
    }
}
