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
        metrics.addEmitListener(() -> {
            var heapInitial = metrics.gauge("jvm_memory_heap_initial", "By");
            var heapUsed = metrics.gauge("jvm_memory_heap_used", "By");
            var heapCommitted = metrics.gauge("jvm_memory_heap_committed", "By");
            var heapMax = metrics.gauge("jvm_memory_heap_max", "By");

            var poolInitial = metrics.gauge("jvm_memory_pool_initial", "By", "pool");
            var poolUsed = metrics.gauge("jvm_memory_pool_used", "By", "pool");
            var poolCommitted = metrics.gauge("jvm_memory_pool_committed", "By", "pool");
            var poolMax = metrics.gauge("jvm_memory_pool_max", "By", "pool");

            return () -> emitMemStats(heapInitial, heapUsed, heapCommitted, heapMax, poolInitial, poolUsed, poolCommitted, poolMax);
        });
    }

    public static void emitGarbageCollectionStatisticsTo(Metrics metrics) {
        metrics.addEmitListener(() -> {
            var gcCount = metrics.gauge("jvm_gc_count", "", "collector");
            var gcTime = metrics.gauge("jvm_gc_time", "", "collector");

            return () -> emitGcStats(gcCount, gcTime);
        });
    }

    private static void emitMemStats(Gauge heapInitial,
                                     Gauge heapUsed,
                                     Gauge heapCommitted,
                                     Gauge heapMax,
                                     Gauge poolInitial,
                                     Gauge poolUsed,
                                     Gauge poolCommitted,
                                     Gauge poolMax) {
        // Get the MemoryMXBean
        var memoryMXBean = ManagementFactory.getMemoryMXBean();

        // Get heap memory usage
        var heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        emitMemUsage(heapInitial, heapUsed, heapCommitted, heapMax, heapMemoryUsage, "");

        // Get memory pools
        var memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (var memoryPoolMXBean : memoryPoolMXBeans) {
            var usage = memoryPoolMXBean.getUsage();
            var sanitizedName = sanitizeName(memoryPoolMXBean.getName());
            emitMemUsage(poolInitial, poolUsed, poolCommitted, poolMax, usage, sanitizedName);
        }
    }

    private static void emitMemUsage(Gauge memInitial,
                                     Gauge memUsed,
                                     Gauge memCommitted,
                                     Gauge memMax,
                                     MemoryUsage memUsage,
                                     String poolName) {
        if (memUsage.getInit() != -1) {
            memInitial.emit(poolName, memUsage.getInit());
        }
        memUsed.emit(poolName, memUsage.getUsed());
        memCommitted.emit(poolName, memUsage.getCommitted());
        if (memUsage.getMax() != -1) {
            memMax.emit(poolName, memUsage.getMax());
        }
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
