package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableSupplier;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;

public class SerializableLazySync2<T> implements SerializableLazy<T> {
    @Serial
    private static final long serialVersionUID = -73562165381170835L;

    private final Serializable writeLock = new Serializable() {};

    private final SerializableSupplier<? extends T> supplier;

    private transient volatile T value;

    public SerializableLazySync2(SerializableSupplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T getValue() {
        var current = value;
        if (current != null) {
            return current;
        }
        synchronized (writeLock) {
            if (value == null) {
                value = supplier.getValue();
            }
            return value;
        }
    }

    @Override
    public void apply(Consumer<? super T> f) {
        var current = value;
        if (current != null) {
            f.accept(current);
        }
    }

    @Override
    public void clear() {
        synchronized (writeLock) {
            value = null;
        }
    }
}