package com.nikolaybotev.metrics.gcloud;

import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.gcloud.counter.CounterAggregatorWriter;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudGauge implements Gauge {
    @Serial
    private static final long serialVersionUID = -720626437577574146L;

    private final GCloudMetrics metrics;

    private final String name;

    private final SerializableLazy<? extends CounterAggregatorWriter> aggregator;

    public GCloudGauge(GCloudMetrics metrics, String name, SerializableLazy<? extends CounterAggregatorWriter> aggregator) {
        this.metrics = metrics;
        this.name = name;
        this.aggregator = aggregator;
    }

    @Override
    public void emit(long observation) {
        aggregator.getValue().add(observation);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getGauge(name);
    }
}
