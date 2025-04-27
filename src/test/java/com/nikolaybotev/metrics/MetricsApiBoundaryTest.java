package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.buckets.ExponentialBuckets;
import com.nikolaybotev.metrics.cloudmonitoring.GCloudMetrics;

import java.util.HashMap;
import java.util.Map;

public class MetricsApiBoundaryTest {
    public static void main(String[] args) {
        var projectId = "feelinsosweet";

        var name = ProjectName.of(projectId);

        var createRequest = CreateTimeSeriesRequest.newBuilder()
                .setName(name.toString())
                .build();
        // Prepares the monitored resource descriptor
        Map<String, String> resourceLabels = new HashMap<>();
        resourceLabels.put("instance_id", "1234567890123456789");
        resourceLabels.put("zone", "us-central1-f");
        var resource = MonitoredResource.newBuilder()
                .setType("gce_instance")
                .putAllLabels(resourceLabels)
                .build();

        // See https://cloud.google.com/logging/docs/logs-based-metrics/distribution-metrics#bucket_layouts
        // and https://cloud.google.com/monitoring/charts/charting-distribution-metrics
        try (var metrics = new GCloudMetrics(createRequest, resource, "my_news/")) {
            //
            // Linear Distribution:
            // * Start value (a) = 0
            // * Number of buckets (N) = 3
            // * Bucket width (b) = 10
            //
            // (-INF, 0), [0, 10), [10, 20), [20, 30), [30, +INF)
            //     ^         ^         ^         ^         ^
            //     0         1         2         3         4
            //
            var linear = metrics.distribution("test_distribution_boundary", 10, 3);

            // Emit some samples to the linear distribution

            // Bucket 0 - (-INF, 0) - 2 samples
            linear.update(-15);
            linear.update(-5);

            // Bucket 1 - [0, 10) - 3 samples
            linear.update(5);
            linear.update(5);
            linear.update(5);

            // Bucket 2 - [10, 20) - 4 samples
            linear.update(15);
            linear.update(15);
            linear.update(15);
            linear.update(15);

            // Bucket 3 - [20, 30) - 5 samples
            linear.update(25);
            linear.update(25);
            linear.update(25);
            linear.update(25);
            linear.update(25);

            // Bucket 4 - [30, +INF) - 6 samples
            linear.update(35);
            linear.update(36);
            linear.update(37);
            linear.update(38);
            linear.update(39);
            linear.update(45);

            //
            // Exponential Distribution:
            // * Number of buckets (N) = 4
            // * Linear scale (a) = 3
            // * Exponential growth factor (b) = 2
            //
            // (-INF, 3), [3, 6), [6, 12), [12, 24), [24, 48), [48, +INF)
            //     ^        ^       ^         ^         ^         ^
            //     0        1       2         3         4         5
            //
            var exponential = metrics.distribution("test_distribution_boundary/exponential",
                    new ExponentialBuckets(4, 2, 3));

            // Bucket 0 - (-INF, 3) - 3 samples
            exponential.update(0);
            exponential.update(1);
            exponential.update(2);

            // Bucket 1 - [3, 6) - 3 samples
            exponential.update(3);
            exponential.update(4);
            exponential.update(5);

            // Bucket 2 - [6, 12) - 6 samples
            for (var i = 6; i < 12; i++) {
                exponential.update(i);
            }

            // Bucket 3 - [12, 24) - 24 samples
            for (var i = 12; i < 24; i++) {
                exponential.update(i);
                exponential.update(i);
            }

            // Bucket 4 - [24, 48) - 12 samples
            for (var i = 24; i < 48; i += 2) {
                exponential.update(i);
            }

            // Bucket 5 - [48, +INF) - 148 samples
            for (var i = 48; i < 196; i++) {
                exponential.update(i);
            }

            metrics.flush();
            System.out.println("Time series created successfully.");
        }
    }
}