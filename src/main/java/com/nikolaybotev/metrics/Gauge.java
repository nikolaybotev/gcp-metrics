package com.nikolaybotev.metrics;

import java.io.Serializable;

public interface Gauge extends Serializable {
    void emit(long observation);
}
