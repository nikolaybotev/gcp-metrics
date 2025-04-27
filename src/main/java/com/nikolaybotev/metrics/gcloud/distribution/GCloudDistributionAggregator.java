package com.nikolaybotev.metrics.gcloud.distribution;

import com.google.api.Distribution;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.nikolaybotev.metrics.gcloud.distribution.aggregator.DistributionAggregatorReader;
import com.nikolaybotev.metrics.gcloud.emitter.GCloudMetricAggregator;

import java.util.stream.LongStream;

public class GCloudDistributionAggregator implements GCloudMetricAggregator {
    private final TimeSeries timeSeriesTemplate;
    private final Distribution.BucketOptions bucketOptions;
    private final DistributionAggregatorReader aggregator;

    public GCloudDistributionAggregator(TimeSeries timeSeriesTemplate, Distribution.BucketOptions bucketOptions, DistributionAggregatorReader aggregator) {
        this.timeSeriesTemplate = timeSeriesTemplate;
        this.bucketOptions = bucketOptions;
        this.aggregator = aggregator;
    }

    @Override
    public TimeSeries getTimeSeriesTemplate() {
        return timeSeriesTemplate;
    }

    @Override
    public TypedValue getAndClear() {
        var distribution = aggregator.getAndClear();

        var distributionValue = Distribution.newBuilder()
                .setCount(distribution.numSamples())
                .setMean(distribution.mean())
                .setSumOfSquaredDeviation(distribution.sumOfSquaredDeviation())
                .setBucketOptions(bucketOptions)
                .addAllBucketCounts(LongStream.of(distribution.buckets()).boxed().toList())
                .build();

        return TypedValue.newBuilder()
                .setDistributionValue(distributionValue)
                .build();
    }
}
