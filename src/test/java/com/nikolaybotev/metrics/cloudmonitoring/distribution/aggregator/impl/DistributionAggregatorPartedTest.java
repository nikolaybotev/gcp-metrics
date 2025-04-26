package com.nikolaybotev.metrics.cloudmonitoring.distribution.aggregator.impl;

import com.nikolaybotev.metrics.buckets.LinearBuckets;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributionAggregatorPartedTest {
    @Test
    public void getAndClear_withSameInputsAsSyncAggregator_returnsSameStats() {
        var threads = Runtime.getRuntime().availableProcessors() * 4;
        var tasks = threads * 1_000;
        var bucketsDef = new LinearBuckets(0, 100, 100);
        var baselineAggregator = new DistributionAggregatorSync(bucketsDef);
        var partedAggregator = new DistributionAggregatorParted(bucketsDef);

        var sum = new AtomicLong();
        try (var executor = Executors.newFixedThreadPool(threads)) {
            for (var i = 0; i < tasks; i++) {
                executor.submit(() -> {
                    var rand = new Random();

                    var sample = rand.nextLong(20_000);
                    sum.addAndGet(sample);
                    baselineAggregator.add(sample);
                    partedAggregator.add(sample);
                });
            }
        }

        var baselineResult = baselineAggregator.getAndClear();
        var partedResult = partedAggregator.getAndClear();

        assertEquals(tasks, baselineResult.numSamples());
        assertEquals(tasks, partedResult.numSamples());
        assertEquals(sum.get() / (double) tasks, baselineResult.mean(), 0.1e5);
        assertEquals(baselineResult.mean(), partedResult.mean(), 0.1e5);
        assertEquals(baselineResult.sumOfSquaredDeviation(), partedResult.sumOfSquaredDeviation(), 0.1e5);
    }
}
