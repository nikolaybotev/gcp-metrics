package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import java.io.Serializable;

public interface SerializableSupplier<T> extends Serializable {
    T getValue();
}
