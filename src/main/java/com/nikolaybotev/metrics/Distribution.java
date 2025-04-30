package com.nikolaybotev.metrics;

import java.io.Serializable;

public interface Distribution extends Serializable {
    default void update(String labelValue, long value) {
        update(value, labelValue);
    }

    void update(long value, String... labelValue);
}
