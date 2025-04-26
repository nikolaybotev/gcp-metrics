package com.nikolaybotev.metrics.cloudmonitoring.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;

public class SerializableLazy<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -73562165381170835L;

    private final FastReadWriteLock lock = new FastReadWriteLock();

    private final SerializableSupplier<T> supplier;

    private transient volatile T value;

    public SerializableLazy(SerializableSupplier<T> supplier) {
        this.supplier = supplier;
    }

    public T getValue() {
        // Optimize for common case - concurrent read lock.
        try (var ignored = lock.acquireReadLock()) {
            if (value != null) {
                return value;
            }
        }

        // Initial startup case - must initialize the value.
        try (var ignored = lock.acquireWriteLock()) {
            if (value != null) {
                return value;
            }
            value = supplier.getValue();
            return value;
        }
    }

    public void apply(Consumer<T> f) {
        try (var ignored = lock.acquireReadLock()) {
            if (value != null) {
                f.accept(value);
            }
        }
    }

    public void clear() {
        try (var ignored = lock.acquireWriteLock()) {
            value = null;
        }
    }
}