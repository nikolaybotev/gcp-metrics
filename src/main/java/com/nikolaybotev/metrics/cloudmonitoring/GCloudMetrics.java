package com.nikolaybotev.metrics.cloudmonitoring;

import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.TimeSeries;
import com.nikolaybotev.metrics.*;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterWithLabelAggregators;
import com.nikolaybotev.metrics.cloudmonitoring.counter.GCloudCounterAggregator;
import com.nikolaybotev.metrics.cloudmonitoring.counter.impl.CounterAggregatorAtomic;
import com.nikolaybotev.metrics.cloudmonitoring.counter.impl.CounterAggregatorParted;
import com.nikolaybotev.metrics.cloudmonitoring.counter.impl.GaugeAggregator;
import com.nikolaybotev.metrics.cloudmonitoring.distribution.GCloudBucketOptions;
import com.nikolaybotev.metrics.cloudmonitoring.distribution.GCloudDistributionAggregator;
import com.nikolaybotev.metrics.cloudmonitoring.distribution.aggregator.impl.DistributionAggregatorParted;
import com.nikolaybotev.metrics.cloudmonitoring.emitter.GCloudMetricsEmitter;
import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableRunnable;
import com.nikolaybotev.metrics.cloudmonitoring.util.lazy.SerializableLazy;
import com.nikolaybotev.metrics.cloudmonitoring.util.lazy.SerializableLazySync;
import com.nikolaybotev.metrics.cloudmonitoring.util.lazy.SerializableSupplier;
import com.nikolaybotev.metrics.cloudmonitoring.util.retry.RetryOnExceptions;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GCloudMetrics implements Metrics, AutoCloseable {
    @Serial
    private static final long serialVersionUID = 7949282012082034388L;

    private static final SerializableSupplier<MetricServiceSettings> DEFAULT_METRICS_SERVICE_SETTINGS =
            new SerializableSupplier<>() {
                @Override
                public MetricServiceSettings getValue() {
                    try {
                        return MetricServiceSettings.newBuilder().build();
                    } catch (IOException ex) {
                        throw new RuntimeException("Error creating client settings.", ex);
                    }
                }

                @Serial
                private Object readResolve() {
                    return DEFAULT_METRICS_SERVICE_SETTINGS;
                }
            };
    private static final Duration DEFAULT_EMIT_INTERVAL = Duration.ofSeconds(10);
    private static final RetryOnExceptions DEFAULT_EMIT_RETRY_POLICY =
            new RetryOnExceptions(Duration.ofSeconds(5), 3, Duration.ofMillis(250));

    private static final ConcurrentHashMap<GCloudMetrics, GCloudMetrics> cache = new ConcurrentHashMap<>();

    private final SerializableSupplier<MetricServiceSettings> metricServiceSettingsSupplier;
    private final CreateTimeSeriesRequest requestTemplate;
    private final MonitoredResource resource;
    private final String metricsPrefix;
    private final Duration emitInterval;
    private final RetryOnExceptions emitRetryPolicy;

    private final List<SerializableRunnable> emitListeners;
    private final SerializableLazy<GCloudMetricsEmitter> emitter;

    private transient ConcurrentHashMap<String, GCloudCounter> counters;
    private transient ConcurrentHashMap<String, GCloudCounterWithLabel> countersWithLabel;
    private transient ConcurrentHashMap<String, GCloudGauge> gauges;
    private transient ConcurrentHashMap<String, GCloudGaugeWithLabel> gaugesWithLabel;
    private transient ConcurrentHashMap<String, GCloudDistribution> distributions;

    public GCloudMetrics(CreateTimeSeriesRequest requestTemplate,
                         MonitoredResource resource,
                         String metricsPrefix) {
        this(DEFAULT_METRICS_SERVICE_SETTINGS, requestTemplate, resource, metricsPrefix, DEFAULT_EMIT_INTERVAL,
                DEFAULT_EMIT_RETRY_POLICY);
    }

    public GCloudMetrics(SerializableSupplier<MetricServiceSettings> metricServiceSettingsSupplier,
                         CreateTimeSeriesRequest requestTemplate,
                         MonitoredResource resource,
                         String metricsPrefix,
                         Duration emitInterval,
                         RetryOnExceptions emitRetryPolicy) {
        this.metricServiceSettingsSupplier = metricServiceSettingsSupplier;
        this.requestTemplate = requestTemplate;
        this.resource = resource;
        this.metricsPrefix = metricsPrefix;
        this.emitInterval = emitInterval;
        this.emitRetryPolicy = emitRetryPolicy;

        this.emitListeners = new CopyOnWriteArrayList<>();
        this.emitter = new SerializableLazySync<>(this::createEmitter);

        initialize();
    }

    private GCloudMetricsEmitter createEmitter() {
        try {
            var client = MetricServiceClient.create(metricServiceSettingsSupplier.getValue());
            return new GCloudMetricsEmitter(client, requestTemplate, emitInterval, emitRetryPolicy, emitListeners, this);
        } catch (IOException ex) {
            throw new RuntimeException("Error creating client.", ex);
        }
    }

    public void addEmitListener(SerializableRunnable r) {
        emitListeners.add(r);
    }

    public void flush() {
        emitter.apply(GCloudMetricsEmitter::emit);
    }

    public void close() {
        emitter.apply(actualEmitter -> {
            actualEmitter.close();
            this.emitter.clear();
        });
        cache.remove(GCloudMetrics.this);
    }

    @Override
    public Counter counter(String name) {
        return getCounter(name);
    }

    @Override
    public CounterWithLabel counterWithLabel(String name, String labelKey) {
        return getCounterWithLabel(name, labelKey);
    }

    @Override
    public Gauge gauge(String name) {
        return getGauge(name);
    }

    @Override
    public GaugeWithLabel gaugeWithLabel(String name, String labelKey) {
        return getGaugeWithLabel(name, labelKey);
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets) {
        return getDistribution(name, unit, buckets);
    }

    GCloudCounter getCounter(String name) {
        return counters.computeIfAbsent(name, key -> {
            var lazyAggregators = new SerializableLazySync<>(() -> {
                var aggregator = new CounterAggregatorParted();
                var metric = createMetric(name);
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, MetricDescriptor.ValueType.INT64).build();
                var gcloudAggregator = new GCloudCounterAggregator(timeSeriesTemplate, aggregator);

                emitter.getValue().addAggregator(gcloudAggregator);
                return aggregator;
            });

            return new GCloudCounter(this, name, lazyAggregators);
        });
    }

    GCloudCounterWithLabel getCounterWithLabel(String name, String labelKey) {
        return countersWithLabel.computeIfAbsent(name, key -> {
            var lazyAggregators = new SerializableLazySync<>(() -> new CounterWithLabelAggregators(labelValue -> {
                var aggregator = new CounterAggregatorAtomic();
                var metric = createMetric(name).putLabels(labelKey, labelValue);
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, MetricDescriptor.ValueType.INT64).build();
                var gcloudAggregator = new GCloudCounterAggregator(timeSeriesTemplate, aggregator);

                emitter.getValue().addAggregator(gcloudAggregator);
                return aggregator;
            }));

            return new GCloudCounterWithLabel(this, name, labelKey, lazyAggregators);
        });
    }

    GCloudGauge getGauge(String name) {
        return gauges.computeIfAbsent(name, key -> {
            var lazyAggregators = new SerializableLazySync<>(() -> {
                var aggregator = new GaugeAggregator();
                var metric = createMetric(name);
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, MetricDescriptor.ValueType.INT64).build();
                var gcloudAggregator = new GCloudCounterAggregator(timeSeriesTemplate, aggregator);

                emitter.getValue().addAggregator(gcloudAggregator);
                return aggregator;
            });

            return new GCloudGauge(this, name, lazyAggregators);
        });
    }

    GCloudGaugeWithLabel getGaugeWithLabel(String name, String labelKey) {
        return gaugesWithLabel.computeIfAbsent(name, key -> {
            var lazyAggregators = new SerializableLazySync<>(() -> new CounterWithLabelAggregators(labelValue -> {
                var aggregator = new GaugeAggregator();
                var metric = createMetric(name).putLabels(labelKey, labelValue);
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, MetricDescriptor.ValueType.INT64).build();
                var gcloudAggregator = new GCloudCounterAggregator(timeSeriesTemplate, aggregator);

                emitter.getValue().addAggregator(gcloudAggregator);
                return aggregator;
            }));

            return new GCloudGaugeWithLabel(this, name, labelKey, lazyAggregators);
        });
    }

    GCloudDistribution getDistribution(String name, String unit, Buckets buckets) {
        return distributions.computeIfAbsent(name, key -> {
            var lazyAggregator = new SerializableLazySync<>(() -> {
                var aggregator = new DistributionAggregatorParted(buckets);
                var metric = createMetric(name);
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, MetricDescriptor.ValueType.DISTRIBUTION)
                        .setUnit(unit)
                        .build();
                var bucketOptions = GCloudBucketOptions.from(buckets);
                var gcloudAggregator = new GCloudDistributionAggregator(timeSeriesTemplate, bucketOptions, aggregator);

                emitter.getValue().addAggregator(gcloudAggregator);
                return aggregator;
            });

            return new GCloudDistribution(this, name, unit, buckets, lazyAggregator);
        });
    }

    private TimeSeries.Builder createTimeSeriesTemplate(Metric.Builder metric, MetricDescriptor.ValueType valueType) {
        return TimeSeries.newBuilder()
                .setMetric(metric)
                .setResource(resource)
                .setMetricKind(MetricDescriptor.MetricKind.GAUGE)
                .setValueType(valueType);
    }

    private Metric.Builder createMetric(String name) {
        return Metric.newBuilder()
                .setType("custom.googleapis.com/" + metricsPrefix + name);
    }

    @Serial
    private Object readResolve() throws ObjectStreamException {
        // Check if an instance with the same properties exists in the cache
        var existingInstance = cache.get(this);
        if (existingInstance != null) {
            return existingInstance;
        }

        // If not found, return the current instance and add it to the cache
        initialize();
        return this;
    }

    private void initialize() {
        this.counters = new ConcurrentHashMap<>();
        this.countersWithLabel = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
        this.gaugesWithLabel = new ConcurrentHashMap<>();
        this.distributions = new ConcurrentHashMap<>();
        cache.put(this, this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GCloudMetrics that)) return false;
        return Objects.equals(metricServiceSettingsSupplier, that.metricServiceSettingsSupplier) &&
                Objects.equals(requestTemplate, that.requestTemplate) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(metricsPrefix, that.metricsPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricServiceSettingsSupplier, requestTemplate, resource, metricsPrefix);
    }
}
