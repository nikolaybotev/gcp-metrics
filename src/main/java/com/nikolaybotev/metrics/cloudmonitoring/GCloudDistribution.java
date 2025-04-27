package com.nikolaybotev.metrics.cloudmonitoring;

import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.cloudmonitoring.distribution.aggregator.DistributionAggregatorWriter;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudDistribution implements Distribution {
    @Serial
    private static final long serialVersionUID = -4376922731386890802L;

    private final GCloudMetrics metrics;

    private final String name;

    private final String unit;

    private final Buckets buckets;

    private final SerializableLazy<? extends DistributionAggregatorWriter> aggregator;

    public GCloudDistribution(GCloudMetrics metrics, String name, String unit, Buckets buckets, SerializableLazy<? extends DistributionAggregatorWriter> aggregator) {
        this.metrics = metrics;
        this.name = name;
        this.unit = unit;
        this.buckets = buckets;
        this.aggregator = aggregator;
    }

    @Override
    public void update(long value) {
        aggregator.getValue().add(value);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getDistribution(name, unit, buckets);
    }
}
