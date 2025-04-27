package com.nikolaybotev.metrics.util.lazy;

import java.io.Serializable;
import java.util.function.Consumer;

public interface SerializableLazy<T> extends Serializable {
    T getValue();
    void apply(Consumer<? super T> f);
    void clear();
}
