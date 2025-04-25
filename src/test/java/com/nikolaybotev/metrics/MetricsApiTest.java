package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.cloudmonitoring.GCloudMetrics;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            var counter = metrics.counterWithLabel("my_counter2", "status_code");
            var distribution = metrics.distribution("test_distribution5", 100, 100);

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
            CounterWithLabel deserializedCounter;
            CounterWithLabel deserializedCounter2;
            Distribution deserializedDistribution;
            Distribution deserializedDistribution2;
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMetrics = (Metrics) ois.readObject();
                deserializedDistribution = (Distribution) ois.readObject();
                deserializedCounter = (CounterWithLabel) ois.readObject();
                deserializedMetrics2 = (Metrics) ois.readObject();
                deserializedDistribution2 = (Distribution) ois.readObject();
                deserializedCounter2 = (CounterWithLabel) ois.readObject();
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
            CounterWithLabel deserializedCounter3;
            CounterWithLabel deserializedCounter4;
            Distribution deserializedDistribution3;
            Distribution deserializedDistribution4;
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMetrics3 = (Metrics) ois.readObject();
                deserializedDistribution3 = (Distribution) ois.readObject();
                deserializedCounter3 = (CounterWithLabel) ois.readObject();
            }
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMetrics4 = (Metrics) ois.readObject();
                deserializedDistribution4 = (Distribution) ois.readObject();
                deserializedCounter4 = (CounterWithLabel) ois.readObject();
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
                counter.inc("200", i);
            }
            counter.inc("400", 2200);
            counter.inc("401", 1120);
            counter.inc("403", 1320);
            counter.inc("500", 1105);
            counter.inc("500");

            var basicCounter = metrics.counter("basic_counter");
            for (var i = 0; i < 50; i++) {
                basicCounter.inc(i);
            }

            // Emit some distributions
            for (var i = 0; i < 128; i++) {
                var value = Math.random() * 10_000;
                deserializedDistribution.update((long) value);
                distribution.update((long) value);
            }

            metrics.flush();
            System.out.println("Time series created successfully.");

            // Schedule regular metrics production, 60K points / second... in 60 threads...
            var threads = 60;
            var scheduler = Executors.newScheduledThreadPool(threads, new ThreadFactoryBuilder()
                    .setDaemon(false)
                    .setNameFormat("worker-%d")
                    .build());
            for (var i = 0; i < threads; i++) {
                var n = i;
                scheduler.scheduleAtFixedRate(() -> {
                    var startTime = System.nanoTime();

                    var maxDelay = 50 + Math.random() * 250;
                    for (var j = 0; j < 1_000; j++) {
                        var delay = Math.random() * maxDelay;
                        deserializedDistribution4.update((long) delay);
                    }

                    var elapsedNanos = System.nanoTime() - startTime;
                    System.out.printf("Submitted 1,000 samples from %d in %.3f ms%n", n, elapsedNanos / 1e6d);
                }, 1, 1, TimeUnit.SECONDS);
            }
        }
    }
}