package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.gcloud.GCloudMetrics;
import com.nikolaybotev.metrics.jmx.JmxMetrics;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Map;

public class MetricsApiRetryTest {
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        var projectId = "feelinsosweet";

        var name = ProjectName.of(projectId);

        var createRequest = CreateTimeSeriesRequest.newBuilder()
                .setName(name.toString())
                .build();
        // Prepares the monitored resource descriptor
        var resourceLabels = Map.of(
                "instance_id", "1234567890123456789",
                "zone", "us-central1-f");
        var resource = MonitoredResource.newBuilder()
                .setType("gce_instance")
                .putAllLabels(resourceLabels)
                .build();

        SerializableSupplier<Map<String, String>> globalLabels = () -> {
            String hostname;
            try {
                var ip = InetAddress.getLocalHost();
                hostname = ip.getHostName();
            } catch (UnknownHostException e) {
                hostname = "unknown";
            }
            return Map.of("hostname", hostname);
        };

        try (var metrics = new GCloudMetrics(createRequest, resource, globalLabels, "my_news/", Duration.ofSeconds(3))) {
            // Serialize the objects
            var serializedMetrics = new ByteArrayOutputStream();
            try (var oos = new ObjectOutputStream(serializedMetrics)) {
                oos.writeObject(metrics);
            }

            metrics.close();

            // Deserialize the objects
            Metrics deserializedMetrics;
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMetrics = (Metrics) ois.readObject();
            }

            deserializedMetrics.counter("dummy").inc();

            // Print and schedule mem stat metric collection
            JmxMetrics.emitAllStatisticsTo(deserializedMetrics);

            Thread.sleep(Duration.ofMinutes(5));
        }
    }
}