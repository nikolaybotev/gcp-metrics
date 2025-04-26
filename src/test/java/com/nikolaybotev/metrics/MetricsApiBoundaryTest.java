package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.cloudmonitoring.GCloudMetrics;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsApiBoundaryTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
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

        try (var metrics = new GCloudMetrics(createRequest, resource, "my_news/")) {
            var distribution = metrics.distribution("test_distribution_boundary", 10, 3);

            // Emit some distributions
            distribution.update(-15);
            distribution.update(-5);

            distribution.update(5);
            distribution.update(5);
            distribution.update(5);

            distribution.update(15);
            distribution.update(15);
            distribution.update(15);
            distribution.update(15);

            distribution.update(25);
            distribution.update(25);
            distribution.update(25);
            distribution.update(25);
            distribution.update(25);

            distribution.update(35);
            distribution.update(35);
            distribution.update(35);
            distribution.update(35);
            distribution.update(35);
            distribution.update(45);

            metrics.flush();
            System.out.println("Time series created successfully.");
        }
    }
}