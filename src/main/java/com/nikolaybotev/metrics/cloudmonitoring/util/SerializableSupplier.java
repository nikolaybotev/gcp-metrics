package com.nikolaybotev.metrics.cloudmonitoring.util;

import java.io.Serializable;

public interface SerializableSupplier<T> extends Serializable {
    T getValue();
}
