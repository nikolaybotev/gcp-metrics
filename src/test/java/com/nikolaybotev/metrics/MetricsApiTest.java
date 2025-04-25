package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.cloudmonitoring.GCloudMetrics;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MetricsApiTest {
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
            var counter = metrics.counter("my_counter", "status_code");
            var distribution = metrics.distribution("test_distribution4", 100, 100);

            // Serialize the objects
            var serializedMetrics = new ByteArrayOutputStream();
            try (var oos = new ObjectOutputStream(serializedMetrics)) {
                oos.writeObject(metrics);
                oos.writeObject(distribution);
                oos.writeObject(counter);
                oos.writeObject(metrics);
                oos.writeObject(distribution);
                oos.writeObject(counter);
            }

            // Deserialize the objects
            Metrics deserializedMetrics;
            Metrics deserializedMetrics2;
            Counter deserializedCounter;
            Counter deserializedCounter2;
            Distribution deserializedDistribution;
            Distribution deserializedDistribution2;
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMetrics = (Metrics) ois.readObject();
                deserializedDistribution = (Distribution) ois.readObject();
                deserializedCounter = (Counter) ois.readObject();
                deserializedMetrics2 = (Metrics) ois.readObject();
                deserializedDistribution2 = (Distribution) ois.readObject();
                deserializedCounter2 = (Counter) ois.readObject();
            }

            System.out.println("metrics and deserializedMetrics are the same instance: " + (metrics == deserializedMetrics));
            System.out.println("metrics and deserializedMetrics2 are the same instance: " + (metrics == deserializedMetrics2));
            System.out.println("counter and deserializedCounter are the same instance: " + (counter == deserializedCounter));
            System.out.println("counter and deserializedCounter2 are the same instance: " + (counter == deserializedCounter2));
            System.out.println("distribution and deserializedDistribution dist are the same instance: " + (distribution == deserializedDistribution));
            System.out.println("distribution and deserializedDistribution2 dist are the same instance: " + (distribution == deserializedDistribution2));

            System.out.println("Close metrics");
            metrics.close();

            Metrics deserializedMetrics3;
            Metrics deserializedMetrics4;
            Counter deserializedCounter3;
            Counter deserializedCounter4;
            Distribution deserializedDistribution3;
            Distribution deserializedDistribution4;
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMetrics3 = (Metrics) ois.readObject();
                deserializedDistribution3 = (Distribution) ois.readObject();
                deserializedCounter3 = (Counter) ois.readObject();
            }
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMetrics4 = (Metrics) ois.readObject();
                deserializedDistribution4 = (Distribution) ois.readObject();
                deserializedCounter4 = (Counter) ois.readObject();
            }

            // Check if the deserialized instance is the same as the original
            System.out.println("metrics and deserializedMetrics3 are the same instance: " + (metrics == deserializedMetrics3));
            System.out.println("metrics and deserializedMetrics4 are the same instance: " + (metrics == deserializedMetrics4));
            System.out.println("deserializedMetrics and deserializedMetrics3 are the same instance: " + (deserializedMetrics == deserializedMetrics3));
            System.out.println("deserializedMetrics3 and deserializedMetrics4 are the same instance: " + (deserializedMetrics3 == deserializedMetrics4));
            System.out.println("counter and deserializedCounter3 are the same instance: " + (counter == deserializedCounter3));
            System.out.println("counter and deserializedCounter4 are the same instance: " + (counter == deserializedCounter4));
            System.out.println("deserializedCounter and deserializedCounter3 are the same instance: " + (deserializedCounter == deserializedCounter3));
            System.out.println("deserializedCounter3 and deserializedCounter4 are the same instance: " + (deserializedCounter3 == deserializedCounter4));
            System.out.println("distribution and deserializedDistribution3 dist are the same instance: " + (distribution == deserializedDistribution3));
            System.out.println("distribution and deserializedDistribution4 dist are the same instance: " + (distribution == deserializedDistribution4));
            System.out.println("deserializedDistribution and deserializedDistribution3 dist are the same instance: " + (deserializedDistribution == deserializedDistribution3));
            System.out.println("deserializedDistribution3 and deserializedDistribution4 dist are the same instance: " + (deserializedDistribution3 == deserializedDistribution4));

            // Emit some counters
            for (var i = 0; i < 100; i++) {
                counter.inc(i, "200");
            }
            counter.inc(2200, "400");
            counter.inc(1120, "401");
            counter.inc(1320, "403");
            counter.inc(1105, "500");

            // Emit some distributions
            for (var i = 0; i < 128; i++) {
                var value = Math.random() * 10_000;
                deserializedDistribution.update((long) value);
                distribution.update((long) value);
            }

            metrics.flush();
            System.out.println("Time series created successfully.");
        }
    }
}