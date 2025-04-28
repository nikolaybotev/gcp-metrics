package com.nikolaybotev.metrics.noop;

import org.junit.jupiter.api.Test;

public class NoopMetricsTest {
    @Test
    public void canConstructNoopMetrics() {
        var noop = new NoopMetrics();

        noop.counter("abc").inc();
        noop.flush();
    }
}
