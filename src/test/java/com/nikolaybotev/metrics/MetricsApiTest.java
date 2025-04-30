package com.nikolaybotev.metrics;

import com.google.api.MonitoredResource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.nikolaybotev.metrics.gcloud.GCloudMetrics;
import com.nikolaybotev.metrics.jmx.JmxMetrics;
import com.nikolaybotev.metrics.prefixed.PrefixedMetrics;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;

import java.io.*;
import java.lang.management.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

        try (var metrics = new GCloudMetrics(createRequest, resource, globalLabels, "my_news/")) {
            var counter = metrics.counter("my_counter2", "", "status_code", "delayed");
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
                counter.inc("200", i);
            }
            counter.inc("400", 2200);
            counter.inc("401", 1120);
            counter.inc("403", 1320);
            counter.inc("500", 1105);
            counter.inc("500");
            counter.inc(100, "200", "yes");
            counter.inc(250, "200", "no");
            counter.inc(8800); // no labels!

            var basicCounter = metrics.counter("basic_counter");
            for (var i = 0; i < 50; i++) {
                basicCounter.inc(i);
            }
            basicCounter.inc();

            // Emit some distributions
            for (var i = 0; i < 128; i++) {
                var value = Math.random() * 10_000;
                deserializedDistribution.update((long) value);
                distribution.update((long) value);
            }

            metrics.flush();
            System.out.println("Time series created successfully.");

            // Print and schedule mem stat metric collection
            printMemStats();
            JmxMetrics.emitAllStatisticsTo(deserializedMetrics4);

            // Schedule regular metrics production, 60K points / second... in 60 threads...
            // Gather stats for metric submission time...
            var submitTimeMicros = deserializedMetrics4.distribution("thousand_point_submit_time", "us", 2_500, 200);
            var submitTimeMillis = deserializedMetrics4.distribution("thousand_point_submit_time_ms", "ms", 10, 200);
            var prefixedMetrics = new PrefixedMetrics(deserializedMetrics4, "Prefix_");
            var sampleSum = prefixedMetrics.counter("thousand_point_submit_gauge", "", "status");
            var sampleGauge = deserializedMetrics4.gauge("gauge_thousand_oaks");
            var threads = 100;
            var samplesPerThread = 100_000;
            var scheduler = Executors.newScheduledThreadPool(threads, new ThreadFactoryBuilder()
                    .setDaemon(false)
                    .setNameFormat("worker-%d")
                    .build());
            var localRandom = ThreadLocal.withInitial(Random::new);
            var localAtomic = new AtomicInteger();
            for (var i = 0; i < threads; i++) {
                var n = i;
                scheduler.scheduleAtFixedRate(() -> {
                    var startTime = System.nanoTime();

                    var rand = localRandom.get();
                    var minDelay = rand.nextDouble() * 6_000;
                    var maxDelay = rand.nextDouble() * 6_000;
                    var acc = 0d;
                    for (var j = 0; j < samplesPerThread; j++) {
                        var delay = minDelay + rand.nextDouble() * maxDelay;
                        acc += delay;
                        //localAtomic.addAndGet(2);
                        sampleSum.inc((long) delay / 100);
                        deserializedDistribution4.update((long) delay);
                    }
                    sampleSum.inc((long) acc + localAtomic.addAndGet((int) acc % 100));
                    sampleGauge.emit((long) acc);

                    var elapsedNanos = System.nanoTime() - startTime;
                    System.out.printf("Submitted %,d samples from thread %d in %.3f ms%n", samplesPerThread, n, elapsedNanos / 1e6d);
                    submitTimeMicros.update(elapsedNanos / 1_000);
                    submitTimeMillis.update(elapsedNanos / 1_000_000);
                }, 1, 1, TimeUnit.SECONDS);
            }
        }
    }

    private static void printMemStats() {
        // Get the MemoryMXBean
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // Get heap memory usage
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        System.out.println("Heap Memory Usage:");
        System.out.println("  Initial: " + heapMemoryUsage.getInit() + " bytes");
        System.out.println("  Used: " + heapMemoryUsage.getUsed() + " bytes");
        System.out.println("  Committed: " + heapMemoryUsage.getCommitted() + " bytes");
        System.out.println("  Max: " + heapMemoryUsage.getMax() + " bytes");
        System.out.println();

        // Get memory pools
        var poolInitial = 0L;
        var poolUsed = 0L;
        var poolCommitted = 0L;
        var poolMax = 0L;
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            MemoryUsage usage = memoryPoolMXBean.getUsage();
            poolInitial += usage.getInit();
            poolUsed += usage.getUsed();
            poolCommitted += usage.getCommitted();
            poolMax += usage.getMax();
            var sanitizedName = sanitizeName(memoryPoolMXBean.getName());
            System.out.println("Memory Pool: " + memoryPoolMXBean.getName() + " (" + sanitizedName + ")");
            System.out.println("  Initial: " + usage.getInit() + " bytes");
            System.out.println("  Used: " + usage.getUsed() + " bytes");
            System.out.println("  Committed: " + usage.getCommitted() + " bytes");
            System.out.println("  Max: " + usage.getMax() + " bytes");
            System.out.println();
        }

        System.out.println("Memory Pool Total / Heap");
        System.out.printf("  Initial: %.2f / %.2f MB%n", poolInitial / 1_048_576d, heapMemoryUsage.getInit() / 1_048_576d);
        System.out.printf("  Used: %.2f / %.2f MB%n", poolUsed / 1_048_576d, heapMemoryUsage.getUsed() / 1_048_576d);
        System.out.printf("  Committed: %.2f / %.2f MB%n", poolCommitted / 1_048_576d, heapMemoryUsage.getCommitted() / 1_048_576d);
        System.out.printf("  Max: %.2f / %.2f MB", poolMax / 1_048_576d, heapMemoryUsage.getMax() / 1_048_576d);
        System.out.println();

        // Get the list of GarbageCollectorMXBeans
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        System.out.println("Garbage Collector Statistics:");
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            var sanitizedName = sanitizeName(gcBean.getName());
            System.out.println("Name: " + gcBean.getName() + " (" + sanitizedName + ")");
            System.out.println("  Number of Collections: " + gcBean.getCollectionCount());
            System.out.println("  Total Time (ms): " + gcBean.getCollectionTime());
            System.out.println();
        }
    }

    private static String sanitizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}