package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import java.io.Serializable;
import java.util.function.Consumer;

public interface SerializableLazy<T> extends Serializable {
    T getValue();
    void apply(Consumer<T> f);
    void clear();
}
