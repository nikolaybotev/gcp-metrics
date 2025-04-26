package com.nikolaybotev.metrics.cloudmonitoring.distribution.aggregator;

import com.nikolaybotev.metrics.cloudmonitoring.distribution.DistributionBuckets;

public interface DistributionAggregatorReader {
    DistributionBuckets getAndClear();
}
