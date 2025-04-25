package com.nikolaybotev.metrics.cloudmonitoring.counter;

import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableLazy;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class LabeledCounterAggregators {
    private final Function<String, CounterAggregator> aggregatorFactory;
    private final SerializableLazy<CounterAggregator> defaultAggregator;
    private final ConcurrentMap<String, CounterAggregator> aggregators;

    public LabeledCounterAggregators(Function<String, CounterAggregator> aggregatorFactory) {
        this.aggregatorFactory = aggregatorFactory;
        this.defaultAggregator = new SerializableLazy<>(() -> aggregatorFactory.apply(null));
        this.aggregators = new ConcurrentHashMap<>();
    }

    public CounterAggregator getAggregatorForLabelValue(@Nullable String labelValue) {
        if (labelValue == null) {
            return defaultAggregator.getValue();
        }
        return aggregators.computeIfAbsent(labelValue, aggregatorFactory);
    }
}
