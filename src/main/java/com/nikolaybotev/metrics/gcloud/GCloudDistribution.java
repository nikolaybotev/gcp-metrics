package com.nikolaybotev.metrics.gcloud;

import com.google.common.collect.ImmutableList;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.gcloud.distribution.aggregator.DistributionAggregatorWriter;
import com.nikolaybotev.metrics.gcloud.labels.LabelAggregatorWriterRegistry;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public class GCloudDistribution implements Distribution {
    @Serial
    private static final long serialVersionUID = -4376922731386890802L;

    private final GCloudMetrics metrics;
    private final String name;
    private final String unit;
    private final Buckets buckets;
    private final ImmutableList<@NonNull String> labelKey;
    private final SerializableLazy<? extends LabelAggregatorWriterRegistry<? extends DistributionAggregatorWriter>> aggregator;

    public GCloudDistribution(GCloudMetrics metrics, String name, String unit, Buckets buckets, ImmutableList<@NonNull String> labelKey,
                              SerializableLazy<? extends LabelAggregatorWriterRegistry<? extends DistributionAggregatorWriter>> aggregator) {
        this.metrics = metrics;
        this.name = name;
        this.unit = unit;
        this.buckets = buckets;
        this.labelKey = labelKey;
        this.aggregator = aggregator;
    }

    @Override
    public void update(long value, String... labelValue) {
        aggregator.getValue().getAggregatorForLabelValue(labelValue).add(value);
    }

    @Serial
    private Object readResolve() {
        return requireNonNull(metrics).getDistribution(name, unit, buckets, labelKey);
    }
}
