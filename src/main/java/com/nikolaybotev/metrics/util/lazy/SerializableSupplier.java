package com.nikolaybotev.metrics.util.lazy;

import java.io.Serializable;

public interface SerializableSupplier<T> extends Serializable {
    T getValue();
}
