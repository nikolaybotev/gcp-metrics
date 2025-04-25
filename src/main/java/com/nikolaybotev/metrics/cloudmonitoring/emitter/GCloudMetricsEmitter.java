package com.nikolaybotev.metrics.cloudmonitoring.emitter;

import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.util.Timestamps;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GCloudMetricsEmitter implements AutoCloseable {
    private final MetricServiceClient client;
    private final CreateTimeSeriesRequest requestTemplate;

    private final List<GCloudMetricAggregator> aggregators = new CopyOnWriteArrayList<>();

    public GCloudMetricsEmitter(MetricServiceClient client, CreateTimeSeriesRequest requestTemplate) {
        this.client = client;
        this.requestTemplate = requestTemplate;

    }

    public void addAggregator(GCloudMetricAggregator aggregator) {
        aggregators.add(aggregator);
    }

    public void close() {
        client.close();
    }

    public void emit() {
        var request = requestTemplate.toBuilder();

        for (var aggregator : aggregators) {
            var interval = TimeInterval.newBuilder()
                    .setEndTime(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                    .build();

            var value = aggregator.getAndClear();

            var point = Point.newBuilder()
                    .setInterval(interval)
                    .setValue(value)
                    .build();

            var timeSeries = aggregator.getTimeSeriesTemplate().toBuilder()
                    .addPoints(point)
                    .build();

            request.addTimeSeries(timeSeries);
        }

        client.createTimeSeries(request.build());
    }
}
