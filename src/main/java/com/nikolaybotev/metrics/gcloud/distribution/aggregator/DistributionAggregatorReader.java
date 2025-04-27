package com.nikolaybotev.metrics.gcloud.distribution.aggregator;

import com.nikolaybotev.metrics.gcloud.distribution.DistributionBuckets;

public interface DistributionAggregatorReader {
    DistributionBuckets getAndClear();
}
