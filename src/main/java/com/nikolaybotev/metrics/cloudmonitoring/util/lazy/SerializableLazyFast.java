package com.nikolaybotev.metrics.cloudmonitoring.util.lazy;

import com.nikolaybotev.metrics.cloudmonitoring.util.SerializableSupplier;

import java.io.Serial;
import java.util.function.Consumer;

public class SerializableLazyFast<T> implements SerializableLazy<T> {
    @Serial
    private static final long serialVersionUID = -1116779709856742097L;

    private final FasterReadWriteLock lock = new FasterReadWriteLock();

    private final SerializableSupplier<T> supplier;

    private transient volatile T value;

    public SerializableLazyFast(SerializableSupplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T getValue() {
        // Optimize for common case - concurrent read lock.
        try (var ignored = lock.acquireReadLock()) {
            var current = value;
            if (current != null) {
                return current;
            }
        }

        // Initial startup case - must initialize the value.
        try (var ignored = lock.acquireWriteLock()) {
            var current = value;
            if (current != null) {
                return current;
            }
            value = supplier.getValue();
            return value;
        }
    }

    @Override
    public void apply(Consumer<? super T> f) {
        try (var ignored = lock.acquireReadLock()) {
            if (value != null) {
                f.accept(value);
            }
        }
    }

    @Override
    public void clear() {
        try (var ignored = lock.acquireWriteLock()) {
            value = null;
        }
    }
}