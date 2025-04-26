package com.nikolaybotev.metrics.cloudmonitoring.emitter;

import com.google.api.gax.rpc.*;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.util.Timestamps;
import com.nikolaybotev.metrics.CounterWithLabel;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.cloudmonitoring.util.RetryOnExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GCloudMetricsEmitter implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(GCloudMetricsEmitter.class);

    private final MetricServiceClient client;
    private final CreateTimeSeriesRequest requestTemplate;
    private final Duration emitInterval;
    private final RetryOnExceptions retryOnExceptions;
    private final CounterWithLabel emitAttempts;
    private final Distribution emitLatencyMs;

    private final List<GCloudMetricAggregator> aggregators = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService emitTimer;

    public GCloudMetricsEmitter(MetricServiceClient client,
                                CreateTimeSeriesRequest requestTemplate,
                                Duration emitInterval,
                                RetryOnExceptions retryOnExceptions,
                                Metrics metrics) {
        this.client = client;
        this.requestTemplate = requestTemplate;
        this.emitInterval = emitInterval;
        this.retryOnExceptions = retryOnExceptions;

        this.emitAttempts = metrics.counterWithLabel("gcp_metrics/emit_attempts", "status");
        this.emitLatencyMs = metrics.distribution("gcp_metrics/emit_latency_millis", "ms", 0, 20, 50);

        this.emitTimer = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("gcloud-metrics-emitter-%d")
                        .build());

        startEmitTimer();
    }

    private void startEmitTimer() {
        logger.info("Scheduled metrics emitter every {} seconds.", emitInterval.toSeconds());
        this.emitTimer.scheduleAtFixedRate(() -> {
            try {
                emit();
                emitAttempts.inc("success");
            } catch (Exception ex) {
                logger.warn("Failed to emit metrics.", ex);
                emitAttempts.inc(ex.getClass().getSimpleName());
            }
        },
        emitInterval.toMillis(), emitInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void addAggregator(GCloudMetricAggregator aggregator) {
        aggregators.add(aggregator);
    }

    public void close() {
        client.close();
        emitTimer.shutdownNow();
        logger.info("Closed metrics emitter.");
    }

    public void emit() {
        var request = requestTemplate.toBuilder();

        for (var aggregator : aggregators) {
            var interval = TimeInterval.newBuilder()
                    .setEndTime(Timestamps.fromMillis(Instant.now().toEpochMilli()))
                    .build();

            var value = aggregator.getAndClear();

            var point = Point.newBuilder()
                    .setInterval(interval)
                    .setValue(value)
                    .build();

            var timeSeries = aggregator.getTimeSeriesTemplate().toBuilder()
                    .addPoints(point)
                    .build();

            request.addTimeSeries(timeSeries);
        }

        var elapsedMillis = retryOnExceptions.run(
                () -> client.createTimeSeries(request.build()),
                Set.of(InternalException.class,
                        UnavailableException.class,
                        AbortedException.class,
                        DataLossException.class));
        logger.info("Emitted {} time series in {} ms.", request.getTimeSeriesCount(), elapsedMillis);
        emitLatencyMs.update(elapsedMillis);
    }
}
