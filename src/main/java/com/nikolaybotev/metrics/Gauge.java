package com.nikolaybotev.metrics;

import java.io.Serializable;

public interface Gauge extends Serializable {
    default void emit(String labelValue, long observation) {
        emit(observation, labelValue);
    }

    void emit(long observation, String... labelValue);
}
