package com.nikolaybotev.metrics;

import java.io.Serializable;

public interface GaugeWithLabel extends Serializable {
    void emit(String labelValue, long observation);
}
