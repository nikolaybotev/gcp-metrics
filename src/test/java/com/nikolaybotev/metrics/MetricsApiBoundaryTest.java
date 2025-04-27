package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.buckets.ExplicitBuckets;
import com.nikolaybotev.metrics.buckets.ExponentialBuckets;
import com.nikolaybotev.metrics.gcloud.GCloudMetrics;

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
            var linear = metrics.distribution("test_distribution_boundary/linear", 10, 3);

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

            //
            // Explicit Distribution:
            // * Bounds (b) = 10, 20, 40, 90
            //
            // (-INF, 10), [10, 20), [40, 90), [90, +INF)
            //      ^         ^         ^         ^
            //      0         1         2         3
            //
            var explicit = metrics.distribution("test_distribution_boundary/explicit",
                    new ExplicitBuckets(10, 20, 40, 90));

            // Bucket 0 - (-INF, 10) - 3 samples
            explicit.update(0);
            explicit.update(0);
            explicit.update(5);

            // Bucket 1 - [10, 20) - 6 samples
            explicit.update(10);
            explicit.update(11);
            explicit.update(11);
            explicit.update(15);
            explicit.update(18);
            explicit.update(19);

            // Bucket 2 - [20, 40) - 3 samples
            explicit.update(20);
            explicit.update(25);
            explicit.update(39);

            // Bucket 3 - [40, 90) - 4 samples
            explicit.update(40);
            explicit.update(79);
            explicit.update(80);
            explicit.update(89);

            // Bucket 4 - [90, +INF) - 1 sample
            explicit.update(189);

            metrics.flush();
            System.out.println("Time series created successfully.");
        }
    }
}