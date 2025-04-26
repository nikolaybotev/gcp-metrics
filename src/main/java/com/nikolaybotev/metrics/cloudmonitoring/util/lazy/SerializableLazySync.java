package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableSupplier;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;

public class SerializableLazySync<T> implements SerializableLazy<T> {
    @Serial
    private static final long serialVersionUID = -73562165381170835L;

    private final Serializable lock = new Serializable() {};

    private final SerializableSupplier<T> supplier;

    private transient T value;

    public SerializableLazySync(SerializableSupplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T getValue() {
        synchronized (lock) {
            if (value == null) {
                value = supplier.getValue();
            }
            return value;
        }
    }

    @Override
    public void apply(Consumer<T> f) {
        synchronized (lock) {
            if (value != null) {
                f.accept(value);
            }
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            value = null;
        }
    }
}