package com.nikolaybotev.metrics.cloudmonitoring.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;

public class SerializableLazy<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -73562165381170835L;

    private final Serializable lock = new Serializable() {};

    private final SerializableSupplier<T> supplier;

    private transient T value;

    public SerializableLazy(SerializableSupplier<T> supplier) {
        this.supplier = supplier;
    }

    public T getValue() {
        synchronized (lock) {
            if (value == null) {
                value = supplier.getValue();
            }
            return value;
        }
    }

    public void apply(Consumer<T> f) {
        synchronized (lock) {
            if (value != null) {
                f.accept(value);
            }
        }
    }

    public void clear() {
        synchronized (lock) {
            value = null;
        }
    }
}