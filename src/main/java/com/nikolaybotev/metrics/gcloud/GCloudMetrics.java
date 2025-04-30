package com.nikolaybotev.metrics.gcloud;

import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.common.collect.ImmutableList;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.TimeSeries;
import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Gauge;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.gcloud.counter.GCloudCounterAggregator;
import com.nikolaybotev.metrics.gcloud.counter.aggregator.impl.CounterAggregatorParted;
import com.nikolaybotev.metrics.gcloud.counter.aggregator.impl.GaugeAggregator;
import com.nikolaybotev.metrics.gcloud.distribution.GCloudBucketOptions;
import com.nikolaybotev.metrics.gcloud.distribution.GCloudDistributionAggregator;
import com.nikolaybotev.metrics.gcloud.distribution.aggregator.impl.DistributionAggregatorParted;
import com.nikolaybotev.metrics.gcloud.emitter.GCloudMetricsEmitter;
import com.nikolaybotev.metrics.gcloud.labels.LabelAggregatorWriterRegistry;
import com.nikolaybotev.metrics.util.lazy.SerializableLazy;
import com.nikolaybotev.metrics.util.lazy.SerializableLazySync;
import com.nikolaybotev.metrics.util.lazy.SerializableSupplier;
import com.nikolaybotev.metrics.util.retry.RetryOnExceptions;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    private final SerializableSupplier<Map<String, String>> metricLabels;
    private final String metricsPrefix;
    private final Duration emitInterval;
    private final RetryOnExceptions emitRetryPolicy;

    private final List<SerializableSupplier<Runnable>> emitListeners;
    private final SerializableLazy<GCloudMetricsEmitter> emitter;

    private transient ConcurrentHashMap<String, GCloudCounter> counters;
    private transient ConcurrentHashMap<String, GCloudGauge> gauges;
    private transient ConcurrentHashMap<String, GCloudDistribution> distributions;

    public GCloudMetrics(CreateTimeSeriesRequest requestTemplate,
                         MonitoredResource resource,
                         String metricsPrefix) {
        this(DEFAULT_METRICS_SERVICE_SETTINGS, requestTemplate, resource, Map::of, metricsPrefix,
                DEFAULT_EMIT_INTERVAL,
                DEFAULT_EMIT_RETRY_POLICY);
    }

    public GCloudMetrics(CreateTimeSeriesRequest requestTemplate,
                         MonitoredResource resource,
                         SerializableSupplier<Map<String, String>> metricLabels,
                         String metricsPrefix) {
        this(DEFAULT_METRICS_SERVICE_SETTINGS, requestTemplate, resource, metricLabels, metricsPrefix,
                DEFAULT_EMIT_INTERVAL,
                DEFAULT_EMIT_RETRY_POLICY);
    }

    public GCloudMetrics(SerializableSupplier<MetricServiceSettings> metricServiceSettingsSupplier,
                         CreateTimeSeriesRequest requestTemplate,
                         MonitoredResource resource,
                         SerializableSupplier<Map<String, String>> metricLabels,
                         String metricsPrefix,
                         Duration emitInterval,
                         RetryOnExceptions emitRetryPolicy) {
        this.metricServiceSettingsSupplier = metricServiceSettingsSupplier;
        this.requestTemplate = requestTemplate;
        this.resource = resource;
        this.metricLabels = metricLabels;
        this.metricsPrefix = metricsPrefix;
        this.emitInterval = emitInterval;
        this.emitRetryPolicy = emitRetryPolicy;

        this.emitListeners = new CopyOnWriteArrayList<>();
        this.emitter = new SerializableLazySync<>(this::createEmitter);

        initialize();
    }

    private GCloudMetricsEmitter createEmitter() {
        var actualEmitListeners = emitListeners.stream().map(SerializableSupplier::getValue).toList();
        return new GCloudMetricsEmitter(metricServiceSettingsSupplier, requestTemplate, metricLabels, emitInterval,
                emitRetryPolicy, actualEmitListeners, this);
    }

    @Override
    public void addEmitListener(SerializableSupplier<Runnable> listener) {
        emitListeners.add(listener);
    }

    @Override
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
    public Counter counter(String name, String unit, String... labelKey) {
        return getCounter(name, unit, ImmutableList.copyOf(labelKey));
    }

    @Override
    public Gauge gauge(String name, String unit, String... labelKey) {
        return getGauge(name, unit, ImmutableList.copyOf(labelKey));
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets) {
        return getDistribution(name, unit, buckets);
    }

    GCloudCounter getCounter(String name, String unit, ImmutableList<String> labelKey) {
        return counters.computeIfAbsent(name, key -> {
            var lazyAggregators = new SerializableLazySync<>(
                    () -> LabelAggregatorWriterRegistry.create(labelKey.size(), labelValue -> {
                        var aggregator = new CounterAggregatorParted();

                        var metric = createMetricWithLabels(name, labelKey, labelValue);
                        var timeSeriesTemplate = createTimeSeriesTemplate(metric, unit, MetricDescriptor.ValueType.INT64);
                        var gcloudAggregator = new GCloudCounterAggregator(timeSeriesTemplate, aggregator);
                        emitter.getValue().addAggregator(gcloudAggregator);

                        return aggregator;
                    }));

            return new GCloudCounter(this, name, unit, labelKey, lazyAggregators);
        });
    }

    GCloudGauge getGauge(String name, String unit, ImmutableList<String> labelKey) {
        return gauges.computeIfAbsent(name, key -> {
            var lazyAggregators = new SerializableLazySync<>(
                    () -> LabelAggregatorWriterRegistry.create(labelKey.size(), labelValue -> {
                        var aggregator = new GaugeAggregator();

                        var metric = createMetricWithLabels(name, labelKey, labelValue);
                        var timeSeriesTemplate = createTimeSeriesTemplate(metric, unit, MetricDescriptor.ValueType.INT64);
                        var gcloudAggregator = new GCloudCounterAggregator(timeSeriesTemplate, aggregator);
                        emitter.getValue().addAggregator(gcloudAggregator);

                        return aggregator;
                    }));

            return new GCloudGauge(this, name, unit, labelKey, lazyAggregators);
        });
    }

    GCloudDistribution getDistribution(String name, String unit, Buckets buckets) {
        return distributions.computeIfAbsent(name, key -> {
            var lazyAggregator = new SerializableLazySync<>(() -> {
                var aggregator = new DistributionAggregatorParted(buckets);
                var metric = createMetric(name);
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, unit, MetricDescriptor.ValueType.DISTRIBUTION);
                var bucketOptions = GCloudBucketOptions.from(buckets);
                var gcloudAggregator = new GCloudDistributionAggregator(timeSeriesTemplate, bucketOptions, aggregator);

                emitter.getValue().addAggregator(gcloudAggregator);
                return aggregator;
            });

            return new GCloudDistribution(this, name, unit, buckets, lazyAggregator);
        });
    }

    private TimeSeries createTimeSeriesTemplate(Metric.Builder metric, String unit, MetricDescriptor.ValueType valueType) {
        return TimeSeries.newBuilder()
                .setMetric(metric)
                .setResource(resource)
                .setMetricKind(MetricDescriptor.MetricKind.GAUGE)
                .setUnit(unit)
                .setValueType(valueType)
                .build();
    }

    private Metric.Builder createMetric(String name) {
        return Metric.newBuilder()
                .setType("custom.googleapis.com/" + metricsPrefix + name);
    }

    private Metric.Builder createMetricWithLabels(String name, ImmutableList<String> labelKey, ImmutableList<String> labelValue) {
        var metric = createMetric(name);
        for (var i = 0; i < Math.min(labelKey.size(), labelValue.size()); i++) {
            metric.putLabels(labelKey.get(i), labelValue.get(i));
        }
        return metric;
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
        this.gauges = new ConcurrentHashMap<>();
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
