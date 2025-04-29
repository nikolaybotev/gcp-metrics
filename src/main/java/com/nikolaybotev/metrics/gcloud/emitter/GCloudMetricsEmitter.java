package com.nikolaybotev.metrics.gcloud.emitter;

import com.google.api.gax.rpc.AbortedException;
import com.google.api.gax.rpc.DataLossException;
import com.google.api.gax.rpc.InternalException;
import com.google.api.gax.rpc.UnavailableException;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.util.Timestamps;
import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;
import com.nikolaybotev.metrics.util.lazy.SerializableLazySync;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;
import com.nikolaybotev.metrics.util.retry.RetryOnExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GCloudMetricsEmitter implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(GCloudMetricsEmitter.class);

    private final SerializableSupplier<MetricServiceSettings> metricServiceSettingsSupplier;
    private final CreateTimeSeriesRequest requestTemplate;
    private final Duration emitInterval;
    private final RetryOnExceptions retryOnExceptions;
    private final List<Runnable> emitListeners;

    private final Counter emitAttempts;
    private final Distribution emitLatencyMs;

    private final SerializableLazy<MetricServiceClient> client;

    private final List<GCloudMetricAggregator> aggregators = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService emitTimer;
    private final ScheduledExecutorService emitListenerTimer;

    public GCloudMetricsEmitter(SerializableSupplier<MetricServiceSettings> metricServiceSettingsSupplier,
                                CreateTimeSeriesRequest requestTemplate,
                                Duration emitInterval,
                                RetryOnExceptions retryOnExceptions,
                                Collection<Runnable> emitListeners,
                                Metrics metrics) {
        this.metricServiceSettingsSupplier = metricServiceSettingsSupplier;
        this.requestTemplate = requestTemplate;
        this.emitInterval = emitInterval;
        this.retryOnExceptions = retryOnExceptions;
        this.emitListeners = List.copyOf(emitListeners);

        this.emitAttempts = metrics.counter("gcp_metrics/emit_attempts", "", "status");
        this.emitLatencyMs = metrics.distribution("gcp_metrics/emit_latency_millis", "ms", 20, 50);

        this.client = new SerializableLazySync<>(() -> {
            try {
                return MetricServiceClient.create(this.metricServiceSettingsSupplier.getValue());
            } catch (IOException ex) {
                logger.warn("Failed to create client.", ex);
                throw new RuntimeException("Failed to create client.", ex);
            }
        });

        this.emitTimer = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("gcloud-metrics-emitter-%d")
                        .build());
        this.emitListenerTimer = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("gcloud-metrics-emit-listener-%d")
                        .build());

        startEmitTimer();
    }

    private void startEmitTimer() {
        logger.info("Scheduled metrics emitter every {} seconds.", emitInterval.toSeconds());
        this.emitTimer.scheduleAtFixedRate(this::onEmitTimer,
                emitInterval.toMillis(), emitInterval.toMillis(), TimeUnit.MILLISECONDS);
        if (!this.emitListeners.isEmpty()) {
            // Start the emit listener timer right away, but run it at an offset - 1/10th of the emit interval.
            this.emitListenerTimer.scheduleAtFixedRate(this::onEmitListenerTimer,
                    emitInterval.toMillis() / 10, emitInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void onEmitTimer() {
        try {
            emit();
            emitAttempts.inc("success");
        } catch (Exception ex) {
            logger.warn("Failed to emit metrics.", ex);
            emitAttempts.inc(ex.getClass().getSimpleName());
        }
    }

    private void onEmitListenerTimer() {
        for (var listener : emitListeners) {
            try {
                listener.run();
            } catch (Exception ex) {
                logger.warn("Listener exception", ex);
            }
        }
    }

    public void addAggregator(GCloudMetricAggregator aggregator) {
        aggregators.add(aggregator);
    }

    public void close() {
        client.apply(MetricServiceClient::close);
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
                () -> client.getValue().createTimeSeries(request.build()),
                Set.of(InternalException.class,
                        UnavailableException.class,
                        AbortedException.class,
                        DataLossException.class));
        logger.info("Emitted {} time series in {} ms.", request.getTimeSeriesCount(), elapsedMillis);
        emitLatencyMs.update(elapsedMillis);
    }
}
