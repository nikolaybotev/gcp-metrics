package com.nikolaybotev.metrics.gcloud;

import com.google.api.Distribution;
import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.monitoring.v3.*;
import com.google.protobuf.util.Timestamps;
import com.nikolaybotev.metrics.buckets.LinearBuckets;
import com.nikolaybotev.metrics.gcloud.distribution.aggregator.impl.DistributionAggregatorParted;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

public class CloudMonitoringMetricsV3Test {
    public static void main(String[] args) {
        var projectId = "feelinsosweet";

        var name = ProjectName.of(projectId);

        // Prepares a distribution...
        var interval = TimeInterval.newBuilder()
                .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
                .build();

        var aggregator = new DistributionAggregatorParted(new LinearBuckets(0, 100, 100));
        for (var i = 0; i < 128; i++) {
            var value = Math.random() * 10_000;
            aggregator.add((long) value);
        }
        var buckets = aggregator.getAndClear();

        var distributionValue = Distribution.newBuilder()
                .setCount(buckets.numSamples())
                .setMean(buckets.mean())
                .setSumOfSquaredDeviation(buckets.sumOfSquaredDeviation())
                .setBucketOptions(Distribution.BucketOptions.newBuilder()
                        .setLinearBuckets(Distribution.BucketOptions.Linear.newBuilder()
                                .setNumFiniteBuckets(100)
                                .setWidth(100)
                                .setOffset(0)
                                .build())
                        .build())
                .addAllBucketCounts(LongStream.of(buckets.buckets()).boxed().toList())
                .build();

        var value = TypedValue.newBuilder()
                .setDistributionValue(distributionValue)
                .build();
        var point = Point.newBuilder()
                .setInterval(interval)
                .setValue(value)
                .build();

        List<Point> pointList = new ArrayList<>();
        pointList.add(point);

        // Prepares the metric descriptor
        Map<String, String> metricLabels = new HashMap<>();
        var metric = Metric.newBuilder()
                .setType("custom.googleapis.com/test_distribution")
                .putAllLabels(metricLabels)
                .build();

        // Prepares the monitored resource descriptor
        Map<String, String> resourceLabels = new HashMap<>();
        resourceLabels.put("instance_id", "1234567890123456789");
        resourceLabels.put("zone", "us-central1-f");
        var resource = MonitoredResource.newBuilder()
                .setType("gce_instance")
                .putAllLabels(resourceLabels)
                .build();

        // Prepares the time series request
        var timeSeries = TimeSeries.newBuilder()
                .setMetric(metric)
                .setResource(resource)
                .setMetricKind(MetricDescriptor.MetricKind.GAUGE)
                .setValueType(MetricDescriptor.ValueType.DISTRIBUTION)
                .addAllPoints(pointList)
                .build();

        var request = CreateTimeSeriesRequest.newBuilder()
                .setName(name.toString())
                .addTimeSeries(timeSeries)
                .build();

        try {
            // Create a MetricServiceClient
            try (var client = MetricServiceClient.create(MetricServiceSettings.newBuilder().build())) {
                // Send the request
                client.createTimeSeries(request);
                System.out.println("Time series created successfully.");
            }
        } catch (IOException | ApiException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
}