package com.nikolaybotev.metrics.cloudmonitoring;

import com.google.api.Metric;
import com.google.api.MetricDescriptor;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.TimeSeries;
import com.nikolaybotev.metrics.Counter;
import com.nikolaybotev.metrics.Distribution;
import com.nikolaybotev.metrics.Metrics;
import com.nikolaybotev.metrics.buckets.Buckets;
import com.nikolaybotev.metrics.cloudmonitoring.counter.CounterAggregator;
import com.nikolaybotev.metrics.cloudmonitoring.counter.GCloudCounterAggregator;
import com.nikolaybotev.metrics.cloudmonitoring.counter.LabeledCounterAggregators;
import com.nikolaybotev.metrics.cloudmonitoring.distribution.GCloudDistributionAggregator;
import com.nikolaybotev.metrics.cloudmonitoring.distribution.HistogramBucketAggregator;
import com.nikolaybotev.metrics.cloudmonitoring.distribution.ToBucketOptions;
import com.nikolaybotev.metrics.cloudmonitoring.emitter.GCloudMetricsEmitter;
import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableLazy;
import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableSupplier;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GCloudMetrics implements Metrics, AutoCloseable {
    @Serial
    private static final long serialVersionUID = 7949282012082034388L;

    private static final SerializableSupplier<MetricServiceSettings> DEFAULT_METRICS_SERVICE_SETTINGS_SUPPLIER =
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
                    return DEFAULT_METRICS_SERVICE_SETTINGS_SUPPLIER;
                }
            };

    private static final ConcurrentHashMap<GCloudMetrics, GCloudMetrics> cache = new ConcurrentHashMap<>();

    private final SerializableSupplier<MetricServiceSettings> metricServiceSettingsSupplier;
    private final CreateTimeSeriesRequest requestTemplate;
    private final MonitoredResource resource;
    private final String metricsPrefix;

    private final SerializableLazy<GCloudMetricsEmitter> emitter;
    private transient ConcurrentHashMap<String, GCloudCounter> counters;
    private transient ConcurrentHashMap<String, GCloudDistribution> distributions;

    public GCloudMetrics(CreateTimeSeriesRequest requestTemplate,
                         MonitoredResource resource,
                         String metricsPrefix) {
        this(DEFAULT_METRICS_SERVICE_SETTINGS_SUPPLIER, requestTemplate, resource, metricsPrefix);
    }

    public GCloudMetrics(SerializableSupplier<MetricServiceSettings> metricServiceSettingsSupplier,
                         CreateTimeSeriesRequest requestTemplate,
                         MonitoredResource resource,
                         String metricsPrefix) {
        this.metricServiceSettingsSupplier = metricServiceSettingsSupplier;
        this.requestTemplate = requestTemplate;
        this.resource = resource;
        this.metricsPrefix = metricsPrefix;

        this.emitter = new SerializableLazy<>(this::createEmitter);

        initialize();
    }

    private GCloudMetricsEmitter createEmitter() {
        try {
            var client = MetricServiceClient.create(metricServiceSettingsSupplier.getValue());
            return new GCloudMetricsEmitter(client, requestTemplate);
        } catch (IOException ex) {
            throw new RuntimeException("Error creating client.", ex);
        }
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
    public Counter counter(String name, @Nullable String labelKey) {
        return getCounter(name, labelKey);
    }

    @Override
    public Distribution distribution(String name, String unit, Buckets buckets) {
        return getDistribution(name, unit, buckets);
    }

    GCloudCounter getCounter(String name, @Nullable String labelKey) {
        return counters.computeIfAbsent(name, key -> {
            var lazyAggregators = new SerializableLazy<>(() -> new LabeledCounterAggregators(labelValue -> {
                if (labelKey == null && labelValue != null) {
                    throw new IllegalArgumentException("labelKey not provided.");
                }

                var aggregator = new CounterAggregator();
                Metric.Builder metric = createMetric(name);
                if (labelKey != null && labelValue != null) {
                    metric.putLabels(labelKey, labelValue);
                }
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, MetricDescriptor.ValueType.INT64).build();
                var gcloudAggregator = new GCloudCounterAggregator(timeSeriesTemplate, aggregator);

                emitter.getValue().addAggregator(gcloudAggregator);
                return aggregator;
            }));

            return new GCloudCounter(this, name, labelKey, lazyAggregators);
        });
    }

    GCloudDistribution getDistribution(String name, String unit, Buckets buckets) {
        return distributions.computeIfAbsent(name, key -> {
            var lazyAggregator = new SerializableLazy<>(() -> {
                var aggregator = new HistogramBucketAggregator(buckets);
                var metric = createMetric(name);
                var timeSeriesTemplate = createTimeSeriesTemplate(metric, MetricDescriptor.ValueType.DISTRIBUTION)
                        .setUnit(unit)
                        .build();
                var bucketOptions = ToBucketOptions.from(buckets);
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
