package com.nikolaybotev.metrics.jmx;

import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.Metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public final class JmxMetrics {
    public static void emitAllStatisticsTo(Metrics metrics) {
        emitMemoryStatisticsTo(metrics);
        emitGarbageCollectionStatisticsTo(metrics);
    }

    public static void emitMemoryStatisticsTo(Metrics metrics) {
        var memInitial = metrics.gauge("jvm_mem_initial", "pool");
        var memUsed = metrics.gauge("jvm_mem_used", "pool");
        var memCommitted = metrics.gauge("jvm_mem_committed", "pool");
        var memMax = metrics.gauge("jvm_mem_max", "pool");
        metrics.addEmitListener(() -> emitMemStats(memInitial, memUsed, memCommitted, memMax));
    }

    public static void emitGarbageCollectionStatisticsTo(Metrics metrics) {
        var gcCount = metrics.gauge("jvm_gc_count", "collector");
        var gcTime = metrics.gauge("jvm_gc_time", "collector");
        metrics.addEmitListener(() -> emitGcStats(gcCount, gcTime));
    }

    private static void emitMemStats(Gauge memInitial,
                                     Gauge memUsed,
                                     Gauge memCommitted,
                                     Gauge memMax) {
        // Get the MemoryMXBean
        var memoryMXBean = ManagementFactory.getMemoryMXBean();

        // Get heap memory usage
        var heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        emitMemUsage(memInitial, memUsed, memCommitted, memMax, heapMemoryUsage, "heap");

        // Get memory pools
        var memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (var memoryPoolMXBean : memoryPoolMXBeans) {
            var usage = memoryPoolMXBean.getUsage();
            var sanitizedName = sanitizeName(memoryPoolMXBean.getName());
            emitMemUsage(memInitial, memUsed, memCommitted, memMax, usage, sanitizedName);
        }
    }

    private static void emitMemUsage(Gauge memInitial,
                                     Gauge memUsed,
                                     Gauge memCommitted,
                                     Gauge memMax,
                                     MemoryUsage memUsage,
                                     String poolName) {
        memInitial.emit(poolName, memUsage.getInit());
        memUsed.emit(poolName, memUsage.getUsed());
        memCommitted.emit(poolName, memUsage.getCommitted());
        memMax.emit(poolName, memUsage.getMax());
    }

    private static void emitGcStats(Gauge gcCount, Gauge gcTime) {
        // Get the list of GarbageCollectorMXBeans
        var gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (var gcBean : gcBeans) {
            var sanitizedName = sanitizeName(gcBean.getName());
            gcCount.emit(sanitizedName, gcBean.getCollectionCount());
            gcTime.emit(sanitizedName, gcBean.getCollectionTime());
        }
    }

    private static String sanitizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private JmxMetrics() {}
}
