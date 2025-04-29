package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.gcloud.GCloudMetrics;
import com.nikolaybotev.metrics.jmx.JmxMetrics;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MetricsApiSerializationTest {
    private static class MyFn implements Serializable {
        public final Counter c1;
        public final Counter c2;
        public final Gauge g1;
        public final Gauge g2;
        public final Distribution d1;

        public MyFn(Metrics metrics) {
            c1 = metrics.counter("test1");
            c2 = metrics.counter("test2", "", "label");
            g1 = metrics.gauge("test3");
            g2 = metrics.gauge("test4", "", "label");
            d1 = metrics.distribution("hello");
        }
    }

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
            JmxMetrics.emitAllStatisticsTo(metrics);

            var myFn = new MyFn(metrics);

            // Serialize the objects
            var serializedMetrics = new ByteArrayOutputStream();
            try (var oos = new ObjectOutputStream(serializedMetrics)) {
                oos.writeObject(myFn);
            }

            metrics.close();

            // Deserialize the objects
            MyFn deserializedMyFn;
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(serializedMetrics.toByteArray()))) {
                deserializedMyFn = (MyFn) ois.readObject();
            }

            deserializedMyFn.c1.inc();
        }
    }
}